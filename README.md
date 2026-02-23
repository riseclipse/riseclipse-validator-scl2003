# RiseClipse (branch iec61850-6-3-snapshot)

This branch has a specific workflow to build a docker image with tools needed by WG iec61850-6-3. 
It gets the jar from GitHub releases. It is intended for deploying development (SNAPSHOT) versions.

GitHub CLI must be used to run this workflow:

`gh workflow run .github/workflows/Release-On-DockerHub.yml --ref iec61850-6-3-snapshot`
