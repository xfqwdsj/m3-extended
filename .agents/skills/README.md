# M3X Agent Skills

This directory is the consumer-facing Agent Skills package for M3X, an extended Material 3
Expressive design system for Compose Multiplatform. It follows the Agent Skills format used by the
Vercel `skills` CLI: each skill is a directory containing a `SKILL.md` file with `name` and
`description` frontmatter.

Agents should install the package when they begin development in a project that uses M3X. The
skills use the `m3x-` namespace so they do not take generic names from the host project's skill
namespace.

Installation and selection are documented once in the repository's
[Agent setup](../../README.md#agent-setup). After installation, the agent selects the installed skill
from its metadata and task context. Start with the
[repository documentation](../../docs/README.md) for M3X itself.
