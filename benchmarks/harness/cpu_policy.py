"""Require the performance governor throughout every timed JMH fork."""

import os
from pathlib import Path
import shutil
import signal
import subprocess


def require_performance(cpu, value):
    if value != "performance":
        raise RuntimeError(f"CPU {cpu} scaling_governor={value!r}; required 'performance'. Stopping JMH.")


def cpu_policy(cpu):
    base = Path(f"/sys/devices/system/cpu/cpu{cpu}/cpufreq")
    fields = ["scaling_driver", "scaling_governor", "energy_performance_preference",
              "scaling_min_freq", "scaling_max_freq"]
    policy = {field: (base / field).read_text().strip() if (base / field).is_file() else None
              for field in fields}
    require_performance(cpu, policy["scaling_governor"])
    return policy


def host_profile():
    tool = shutil.which("powerprofilesctl")
    if tool:
        p = subprocess.run([tool, "get"], capture_output=True, text=True)
        if p.returncode == 0:
            return p.stdout.strip()
    return None


def start_run(cpu):
    def terminate(signum, _frame):
        raise SystemExit(128 + signum)
    signal.signal(signal.SIGTERM, terminate)
    return cpu_policy(cpu), host_profile()


def terminate_group(process):
    try:
        os.killpg(process.pid, signal.SIGTERM)
    except ProcessLookupError:
        return
    try:
        process.wait(timeout=2)
    except subprocess.TimeoutExpired:
        pass
    try:
        os.killpg(process.pid, signal.SIGKILL)
    except ProcessLookupError:
        pass


def run_checked_fork(cmd, log, cpu):
    before = cpu_policy(cpu)
    governor_file = Path(f"/sys/devices/system/cpu/cpu{cpu}/cpufreq/scaling_governor")
    with subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT, start_new_session=True) as process:
        try:
            while True:
                try:
                    rc = process.wait(timeout=0.5)
                    break
                except subprocess.TimeoutExpired:
                    require_performance(cpu, governor_file.read_text().strip())
            after = cpu_policy(cpu)
        except BaseException as exc:
            log.write(f"\nFork rejected: {exc}\n")
            log.flush()
            terminate_group(process)
            raise
        if rc:
            raise subprocess.CalledProcessError(rc, cmd)
    return before, after
