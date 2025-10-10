#!/bin/bash

# Install git hooks - run once after cloning: ./scripts/setup-hooks.sh

if [ ! -d ".git" ]; then
  echo "Error: Not in a git repository"
  exit 1
fi

cp scripts/git-hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

cp scripts/git-hooks/post-merge .git/hooks/post-merge
chmod +x .git/hooks/post-merge

echo "Git hooks installed (pre-commit: auto-format with ktfmt)"
