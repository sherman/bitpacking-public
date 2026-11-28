import gzip
import io
import json
from contextlib import redirect_stdout
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from harness.runner import ForkSettings, make_schedule, run_schedule


class CollectorTest(unittest.TestCase):
    def test_guard_rejection_keeps_completed_forks_and_stops_collection(self):
        plan = make_schedule([{"name": "decode"}], seed=91, calibration_blocks=1, timing_blocks=1, profile_blocks=1)
        settings = ForkSettings(2, "java", (), 1, "1s", 1, "1s")
        benchmark = "example.Codec.decode"
        jars = {"A": "baseline.jar", "B": "candidate.jar"}
        attempts = []

        def case(entry):
            return entry["name"], benchmark, {}

        with tempfile.TemporaryDirectory() as directory:
            out = Path(directory)

            def launch(cmd, console, cpu):
                self.assertEqual(cmd[:3], ["taskset", "-c", str(cpu)])
                self.assertEqual((out / "calibration.json").exists(), len(attempts) >= 4)
                attempts.append(cmd[cmd.index("-jar") + 1])
                if "-prof" in cmd:
                    console.write("Fork rejected: governor changed\n")
                    raise RuntimeError("governor changed")
                jmh = {"benchmark": benchmark, "threads": 1, "forks": 1, "secondaryMetrics": {},
                       "primaryMetric": {"score": 20, "scoreUnit": "ns/op", "rawData": [[20]]}}
                Path(cmd[cmd.index("-rff") + 1]).write_text(json.dumps([jmh]))
                return ({"scaling_governor": "performance"},) * 2

            with patch("harness.runner.run_checked_fork", side_effect=launch), redirect_stdout(io.StringIO()):
                with self.assertRaisesRegex(RuntimeError, "governor changed"):
                    run_schedule(out, plan, jars, settings, case)

            rows = list(map(json.loads, (out / "forks.jsonl").read_text().splitlines()))
            expected = "AAAA" + plan[1]["order"]
            self.assertEqual(attempts, [jars[side] for side in expected + plan[2]["order"][0]])
            self.assertEqual("".join(row["build"] for row in rows), expected)
            self.assertEqual([row["stage"] for row in rows], ["calibration"] * 4 + ["timing"] * 4)
            with gzip.open(out / "console.txt.gz", "rt") as log:
                self.assertIn("Fork rejected: governor changed", log.read())
