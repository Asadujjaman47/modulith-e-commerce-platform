# GIT_WORKFLOW.md

# Git Workflow

Project: E-Commerce Platform

Version: 1.0

---

# Purpose

This document defines:

* Branching strategy
* Commit conventions
* Pull request process
* Release process
* Branch protection rules

All contributors and AI agents must follow this workflow.

---

# Workflow Strategy

GitHub Flow

Principles:

* Single long-lived branch
* Short-lived feature branches
* Pull-request driven development
* Continuous integration
* Frequent merges

Main branch must always remain deployable.

---

# Branch Structure

main

feature/*

bugfix/*

chore/*

docs/*

---

# Main Branch

Branch:

main

Rules:

* Always stable
* Always deployable
* Protected branch
* No direct commits
* Pull Request required

Forbidden:

git push origin main

Direct pushes are not allowed.

---

# Feature Branches

Naming Convention

feature/<module>-<feature>

Examples

feature/auth-jwt-login

feature/catalog-product-search

feature/order-checkout

feature/payment-refund

feature/shipment-tracking

---

# Bug Fix Branches

Naming Convention

bugfix/<description>

Examples

bugfix/order-cancel-validation

bugfix/payment-timeout

bugfix/cart-quantity-check

---

# Chore Branches

Naming Convention

chore/<description>

Examples

chore/docker-setup

chore/github-actions

chore/java21-upgrade

---

# Documentation Branches

Naming Convention

docs/<description>

Examples

docs/api-guide

docs/module-update

docs/architecture-update

---

# Development Flow

Step 1

Update local main

git checkout main

git pull origin main

---

Step 2

Create branch

git checkout -b feature/catalog-product-management

---

Step 3

Implement changes

Commit frequently

---

Step 4

Push branch

git push -u origin feature/catalog-product-management

---

Step 5

Open Pull Request

Target:

main

---

Step 6

CI Validation

Required:

* Build success
* Unit tests pass
* Integration tests pass
* Modulith verification pass

---

Step 7

Review

Review checklist must pass.

---

Step 8

Squash Merge

Merge into main.

---

Step 9

Delete Branch

Delete remote branch.

Delete local branch.

---

# Pull Request Rules

Every Pull Request must:

* Solve one concern
* Pass CI
* Include tests
* Update documentation if required
* Follow architecture rules

Preferred Size

Less than 500 lines

Maximum

1000 lines

Large changes should be split.

---

# Pull Request Template

Title

feat(order): implement order creation

Description

Summary

Changes

Testing

Documentation Impact

Screenshots (if applicable)

---

# Commit Convention

Use Conventional Commits.

Format

<type>(scope): description

Examples

feat(auth): add jwt login

feat(order): implement order creation

feat(cart): add coupon support

fix(payment): handle timeout response

fix(inventory): prevent negative stock

refactor(catalog): simplify mapper

test(order): add integration tests

docs(api): update order endpoints

chore(docker): update postgres image

---

# Commit Types

feat

New feature

fix

Bug fix

refactor

Code restructuring

test

Tests

docs

Documentation

chore

Maintenance

build

Build changes

ci

CI/CD changes

---

# Merge Strategy

Use:

Squash Merge

Reason:

* Clean history
* Easier rollback
* Simpler release notes

Forbidden:

Merge Commit

Rebase Merge

---

# Release Strategy

Use Git Tags.

Examples

v0.1.0

v0.2.0

v0.3.0

v1.0.0

---

# Release Mapping

v0.1.0

Foundation

---

v0.2.0

Authentication

User Module

---

v0.3.0

Catalog

Inventory

---

v0.4.0

Cart

Coupon

---

v0.5.0

Order

---

v0.6.0

Payment

Shipment

---

v0.7.0

Notifications

Reviews

---

v0.8.0

Reporting

Audit

---

v1.0.0

Production Ready

---

# Branch Protection Rules

main branch protection:

Required:

* Pull Request
* CI Success
* Up-to-date branch
* Review approval

Disabled:

* Force push
* Branch deletion
* Direct commits

---

# CI Requirements

Every Pull Request must execute:

mvn clean verify

Unit Tests

Integration Tests

Modulith Verification

Static Analysis

Docker Build Validation

Any failure blocks merge.

---

# Hotfix Process

For production issues:

Create:

bugfix/critical-payment-issue

Fix issue

Open PR

Merge after approval

Tag release

Example

v1.0.1

---

# AI Agent Rules

Claude Code

Codex

Cursor

OpenCode

must:

* Create feature branches only
* Never commit directly to main
* Follow commit conventions
* Update documentation when required
* Keep PRs small and focused

---

# Definition Of Done

A change is complete only when:

* Code implemented
* Tests pass
* Documentation updated
* CI passes
* PR approved
* Squash merged
* Branch deleted
* Release tag created when applicable
