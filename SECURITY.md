# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.x.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Simple Builders, we appreciate your help in disclosing it to us in a responsible manner.

### How to Report

Please report security issues by [opening a new issue](https://github.com/java-helpers/simple-builders/issues) on GitHub.

### What to Include

When reporting a vulnerability, please include:
- A description of the vulnerability
- Steps to reproduce the issue
- Any mitigation or workaround if known
- Your name/handle for recognition (unless you wish to remain anonymous)

### Our Commitment

- We will confirm the vulnerability and determine its impact
- We will keep you informed of the progress towards resolving the issue
- We will credit you for your discovery (unless you prefer to remain anonymous)

## Security Updates

Security updates will be released as patch versions (e.g., 0.1.1) for the latest minor version. We recommend always using the latest version of the library.

## Dependencies

We regularly update our dependencies to include security fixes, and CI runs an
OWASP dependency-check gate (see `.github/workflows/dependency-check.yml`) that
fails the build on any dependency with a CVSS score of 7 or higher.

You can run the same multi-module scan locally with:

```bash
mvn org.owasp:dependency-check-maven:aggregate
```

Set an [NVD API key](https://nvd.nist.gov/developers/request-an-api-key) via
`-Dnvd.api.key=<key>` to avoid rate limiting.

## Best Practices

When using Simple Builders, we recommend:
1. Always use the latest version
2. Review the code before using in production
3. Follow the principle of least privilege
4. Validate all inputs, especially when using builder patterns with untrusted data

## Security Considerations

While Simple Builders provides utility for creating builders, please be aware that:
- Builders may be used to construct objects with sensitive data
- Always validate inputs before using them in your application
- Be cautious when serializing/deserializing builder-created objects

## License

By contributing to this project, you agree that your contributions will be licensed under its MIT License.
