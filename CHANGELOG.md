# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- ZIO 2.x wrapper for layoutz terminal UI library
- `TerminalService` for effect-typed terminal operations
- `WidgetRenderer` service for widget rendering
- Domain models: `Widget`, `Layout`, `RenderResult`
- Typed error ADT: `TUIError`
- Resource-safe terminal initialization with ZLayer and Scope
- ZIO Test specifications for core services
- Example application demonstrating TUI capabilities

### Changed

- Transformed from zio-quickstart template to zio-tui library
- Updated package structure to `io.github.riccardomerolla.zio.tui`

### Deprecated

### Removed

- Previous quickstart template code

### Fixed

### Security

## [0.1.0] - TBD

### Added

### Features

---

[Unreleased]: https://github.com/riccardomerolla/zio-tui/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/riccardomerolla/zio-tui/releases/tag/v0.1.0
