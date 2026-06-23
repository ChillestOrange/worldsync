package io.github.chillestorange.service.sync;

/**
 * Replaces the "-1" / "0" / "1" strings that used to travel between
 * Comparator.exe (stdout) and orchestrator.sync(direction=...).
 * <p>
 * Contract (verified against Comparator.cpp's actual branches, not the
 * stale docstring at the top of the old file_accesser.py):
 *   NO_OP    -> worlds already in sync, or world names didn't match
 *   UPLOAD   -> local copy is newer, push to Drive
 *   DOWNLOAD -> remote copy is newer, pull from Drive
 */
public enum SyncDirection {
    NO_OP,
    UPLOAD,
    DOWNLOAD
}