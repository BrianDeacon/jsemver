package com.navelplace.jsemver.exceptions

class InvalidVersionFormatException(format: String): RuntimeException("Invalid version format: $format")