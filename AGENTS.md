# Repository Guidance

## Testing

- Tests must verify observable behavior or resulting state. Unit, integration, and end-to-end tests are all appropriate when they exercise code and assert outputs, side effects, persisted data, or public contracts.
- Do not read or search implementation files to assert that particular source text exists. This prohibition includes Java source, SQL migrations and fixtures, templates, build scripts, configuration files, and embedded code stored in those files.
- Do not make tests depend on private implementation details such as method bodies, query fragments, formatting, statement order, or identifier names unless an identifier is itself a documented public contract.
- Test SQL migrations by applying them to a disposable database and then querying the resulting schema or data, or by exercising the constraints, functions, and triggers they install.
- Test embedded programs, including JavaScript stored in database configuration, by executing them with representative inputs and asserting their outputs.
- Reading a test fixture is acceptable when the fixture is an input to the behavior under test. Assertions must be made on the system's observable result, not on the fixture's source text.
- When a text-inspection test is encountered, replace it with behavioral coverage or remove it only when equivalent behavioral coverage already exists.

