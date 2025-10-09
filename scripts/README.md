# Git Hooks

This directory contains git hooks that help maintain code quality.

## Installation

Run this command from the project root:

```bash
./scripts/setup-hooks.sh
```

## Available Hooks

### pre-commit
Automatically runs `ktfmtFormat` before each commit to ensure all code is properly formatted.

**What happens:**
1. When you run `git commit`, the hook runs automatically
2. If your code needs formatting, it will be formatted
3. The commit will be rejected with a message to review and commit again
4. Run `git add -u` to stage the formatted changes
5. Commit again

**Bypassing the hook:**
```bash
git commit --no-verify  # Not recommended
```

## For CI/CD

The hooks only run locally. CI/CD should have its own checks to verify formatting:
```bash
./gradlew ktfmtCheck
```
