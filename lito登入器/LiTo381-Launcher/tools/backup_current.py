"""Verify the sealed 0.6.66 baseline; create it only when it does not exist."""
import baseline_066 as baseline
if baseline.ARCHIVE.exists():baseline.verify()
else:baseline.prepare()
