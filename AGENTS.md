# Astrolabe Wallpaper agent guidance

Read [README.md](README.md) for scope and the live issue backlog. This is an
independent Android project targeting a Samsung Galaxy A57, with a personal APK
first and possible F-Droid distribution later. The bootstrap contains no Android
implementation; build commands and checks will be established in #1.

## Working defaults

- Preserve unrelated changes and existing signed history. Follow current user
  instructions and repository configuration and CI requirements.
- After the initial bootstrap on `main`, develop on issue-linked branches in
  native-filesystem sibling worktrees named
  `astrolabe-wallpaper-<issue-number>-<short-description>`. Use branch names such
  as `feat/1-android-bootstrap`; keep the main checkout on `main`.
- Use GitHub issues to record scope, dependencies, and acceptance criteria. Open a
  pull request for subsequent development and link its issue. Do not claim
  acceptance criteria are met without evidence.
- Keep changes small and readable. Add meaningful tests for behavior changes;
  documentation-only work needs appropriate content and link checks.

## Architecture and product constraints

- Use Kotlin, Canvas, and `WallpaperService` with a small settings app. Keep
  astronomy calculations separable from Android lifecycle and drawing code.
- Use `io.github.cmp0xff.astrolabewallpaper` as the stable release application ID.
- Use Astronomy Engine, pin its version or source revision, and retain its
  notices. Record provenance and licenses for all dependencies and bundled data
  or artwork. Draw original artwork; avoid proprietary SDKs.
- Request current location during initial setup using Android's built-in location
  API and accept approximate results. Handle denial, disabled location, failure,
  and timeout with an offline city chooser or coordinate entry. Persist the
  observing location and provide an explicit refresh from settings; do not add
  continuous background location tracking.
- The clock follows the phone timezone; astronomy uses the saved location.
  Runtime calculations and manual setup must work offline.
- Support home and lit lock screens, subject to physical A57 verification. Always
  On Display and interactive sky exploration are outside the first release.
- Render only while visible. Verify wake, surface recreation, process recreation,
  and time/timezone changes. Record actual firmware when testing the A57.
- Keep signing keys, passwords, local SDK paths, and private device data out of
  Git. Retain and privately back up the first durable APK release key. Do not
  promise F-Droid signature continuity before reproducibility is verified.

## Commit metadata and signing

Use the `git-metadata` skill when available. Subjects and PR titles follow
[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/), for example
`feat(wallpaper): render the astronomical dial`. Link relevant issues in the body
using native GitHub references such as `Refs #5` or `Closes #5` when appropriate.

Every AI-authored commit must include a body recording:

- The motivation or problem being addressed.
- The chosen approach and material constraints.
- Alternatives actually considered and why they were not chosen. If none were
  considered, say so briefly; never manufacture alternatives to fill a section.
- Verification actually performed, with unrun checks and limitations stated.

End each AI-authored commit with **exactly one** `Co-Authored-By` trailer naming
the actual disclosed model and its provider's no-reply address. Use the client
identity only if the model is unavailable. For an OpenAI model, use
`Co-Authored-By: <disclosed model> <noreply@openai.com>` with the actual model name
substituted. Inspect the complete message before committing to prevent duplicate
trailers. Never invent model versions, tests, decisions, or review evidence.

Preserve Git signing. Do not disable signing to work around unavailable agent
access. Confirm the created commit is signed and verify it before the handoff.
The repository-local author identity is
`cmp0xff <5564164+cmp0xff@users.noreply.github.com>`; do not replace it with an
employer identity or change global Git settings.

## Pull requests and Accountability Index

Follow [.github/pull_request_template.md](.github/pull_request_template.md):

- Lead with two to four concise outcome bullets, a verification checklist, and
  issue links. State unrun checks honestly.
- Add risk, compatibility, migration, or rollout details only when material.
- Include a collapsible **Accountability Index** at the bottom: a linked table of
  each commit with a brief what/why, links to discussion comments containing
  review decisions, and a short caveats note. It indexes evidence; full rationale
  belongs in commit bodies and discussion threads.
- Update the index after each push and review round. Link evidence without
  copying extensive rationale. This project explicitly requires this per-commit
  index, overriding the global default against one.
- Finish with the same visible `Co-Authored-By` identity used for the source work.
  This attribution does not claim it will survive squash merging.
- Use native, unquoted GitHub references for issues, PRs, and commits: #5,
  `owner/repo#number`, or `owner/repo@sha` (substitute actual values and remove
  code formatting in published text). Use direct links to specific discussion
  comments for review evidence. Do not automatically mention or assign reviewers;
  use the structured reviewers field only when authorized.
- Do not rewrite existing commits or live PRs solely to apply metadata policy.

This accountability policy is adapted from
[cmp0xff's pinned guidance](https://github.com/cmp0xff/pandas-stubs/blob/f72b507f64c50427ad50595a7c615b0ac3552b61/AGENTS.md),
with Android conventions replacing the upstream project's domain-specific rules.

## Verification

Until #1 supplies the pinned toolchain and CI, check documentation, issue links,
template structure, ignore rules, and commit metadata. Do not imply that a build,
test APK, or physical-device test exists.

For Android work, run the documented checks appropriate to the change. Astronomy
tests must cite independent reference data, units, coordinate frames, and
tolerances, including hemisphere and polar cases. Device reports must distinguish
physical A57 results from emulator checks. Record limitations and unresolved
failures in the issue and PR; do not silently weaken acceptance criteria.
