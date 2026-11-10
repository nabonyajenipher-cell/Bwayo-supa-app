# Build Android and Windows packages

The repository workflow is at `.github/workflows/bwayo-miracle-thunder.yml` (repository root). Open **Actions**, choose **Build Bwayo Miracle Thunder**, and run the workflow. Download the APK and Windows ZIP from the completed run's Artifacts section.

The workflow generates standard Flutter platform files, analyzes the Dart source, and builds each target on its matching hosted runner. A successful CI build confirms compilation only; real USB discovery and hardware servicing still require implementation and device testing.
