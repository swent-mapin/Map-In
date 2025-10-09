#!/bin/bash

# Setup script to install git hooks for the project
# Run this once after cloning the repository: ./scripts/setup-hooks.sh

HOOKS_DIR="scripts/git-hooks"
GIT_HOOKS_DIR=".git/hooks"

echo "Installing git hooks..."

# Check if we're in a git repository
if [ ! -d ".git" ]; then
  echo "Error: Not in a git repository"
  exit 1
fi

# Install pre-commit hook
if [ -f "$HOOKS_DIR/pre-commit" ]; then
  cp "$HOOKS_DIR/pre-commit" "$GIT_HOOKS_DIR/pre-commit"
  chmod +x "$GIT_HOOKS_DIR/pre-commit"
  echo "Pre-commit hook installed"
else
  echo "Warning: pre-commit hook not found in $HOOKS_DIR"
fi

# Install post-merge hook
if [ -f "$HOOKS_DIR/post-merge" ]; then
  cp "$HOOKS_DIR/post-merge" "$GIT_HOOKS_DIR/post-merge"
  chmod +x "$GIT_HOOKS_DIR/post-merge"
  echo "Post-merge hook installed"
else
  echo "Warning: post-merge hook not found in $HOOKS_DIR"
fi

echo ""
echo "Git hooks setup complete!"
echo ""
echo "The pre-commit hook will automatically:"
echo "  - Run ktfmtFormat before each commit"
echo "  - Prevent commits with unformatted code"
echo ""
echo "To bypass the hook (not recommended): git commit --no-verify"
