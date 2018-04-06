package com.navelplace.jsemver

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VersionTest {

    companion object {
        val validVersions = arrayOf("1.1.0",
                "1.1.0-SNAPSHOT",
                "20.1.1",
                "1.1.1-SNAPSHOT.foo-asdf",
                "1.1.1-NONSENSE",
                "1.1.1-SNAPSHOT+blah",
                "1.1.1-1.1.1.1.1.1.1.1.1")

        val invalidVersions = arrayOf("garbage",
                "1.0",
                "a5.0.0",
                "1.1.1.1",
                "1.1.1a",
                "1.1.1-SNAPSHOT+only_0-9A-Za-z-_is_allowed",
                "1.1.1.",
                "1.1.1-1.",
                "1.1.1-+")
    }

    @Test
    fun `Constructs from static helper method`() {
        validVersions.forEach { assertEquals(it, Version.fromString(it).toString()) }
    }

    @Test
    fun `Correctly instantiates for valid input`() {
        validVersions.forEach { Version(it) }
    }

    @Test
    fun `Fails with an InvalidVersionFormatException when supplied bad input`() {
        invalidVersions.forEach { assertFailsWith(InvalidVersionFormatException::class, { Version(it) }) }
    }

    @Test
    fun `isValid returns true for all valid input`() {
        assertTrue(validVersions.all { Version.isValid(it) })
    }

    @Test
    fun `isValid returns fals for all invalid input`() {
        assertTrue(invalidVersions.all { !Version.isValid(it) })
    }

    @Test
    fun `Can find the major version`(){
        val v1 = arrayOf("1.1.1", "1.2.2", "1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT+foo")
        assertTrue(v1.all { Version(it).major == 1 })
    }

    @Test
    fun `Can find the minor version`(){
        val v2 = arrayOf("0.2.1", "0.2.3", "0.2.0-SNAPSHOT", "1.2.3-foo+bar")
        assertTrue(v2.all { Version(it).minor == 2 })
    }

    @Test
    fun `Can find the patch version`(){
        val v3 = arrayOf("0.2.3", "1.2.3", "0.2.3-SNAPSHOT", "1.2.3-foo+bar")
        assertTrue(v3.all { Version(it).patch == 3 })
    }

    @Test
    fun `Can find the prelease version`() {
        val prereleases = arrayOf("1.1.1-SNAPSHOT", "1.1.1-SNAPSHOT+stuff")
        assertEquals("SNAPSHOT",  Version(prereleases[0]).preRelease)
        assertTrue(prereleases.all { Version(it).preRelease == "SNAPSHOT" } )
    }

    @Test
    fun `Can find the prelease version when it is dotted`() {
        assertEquals("foo.bar",  Version("1.1.1-foo.bar").preRelease)
    }

    @Test
    fun `Can find the build data`() {
        assertEquals("bar", Version("1.1.1-foo+bar").metadata)
    }

    @Test
    fun `Must sort versions correctly`() {
        val inOrder = arrayOf(
                "0.1.0-SNAPSHOT",
                "0.1.0-Snapshot",
                "0.1.0-snapshot",
                "0.1.0",
                "1.0.0-alpha",
                "1.0.0-alpha.1",
                "1.0.0-alpha.1.1",
                "1.0.0-alpha.1.next",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0").map { Version(it) }.toTypedArray()

        val shuffled = inOrder.clone().toList().shuffled().toTypedArray()
        shuffled.sort()
        assertArrayEquals(inOrder, shuffled)

        val reversed = inOrder.clone().reversed().toTypedArray()
        reversed.sort()
        assertArrayEquals(inOrder, reversed)
    }

    @Test
    fun `Can parse a simple semver`() {
        val v = Version.fromString("1.2.3")
        assertEquals(1, v.major)
        assertEquals(2, v.minor)
        assertEquals(3, v.patch)
    }

    @Test(expected = InvalidVersionFormatException::class)
    fun `Can reject a bad semver`() {
        Version.fromString("01.2.3")
    }

    @Test(expected = InvalidVersionFormatException::class)
    fun `Can reject a bad prerelease`() {
        Version.fromString("1.2.3-^^^")
    }

    @Test
    fun `Can parse the prerelease`() {
        assertEquals("SNAPSHOT", Version.fromString("1.2.3-SNAPSHOT").preRelease)
    }

    @Test
    fun `Can parse the build`() {
        assertEquals("123", Version.fromString("1.2.3-SNAPSHOT+123").metadata)
    }

    @Test
    fun `Can handle dashes in the prerelease and metadata`() {
        val version = Version.fromString("1.2.3-abc-def+ghi-jkl")
        assertEquals("abc-def", version.preRelease)
        assertEquals("ghi-jkl", version.metadata)
        assertEquals("ghi-jkl", version.metadataElements[0])
        assertEquals("abc-def", version.preReleaseElements[0])
        assertEquals(1, version.preReleaseElements.size)
        assertEquals(1, version.metadataElements.size)
    }

    @Test
    fun `Can parse the elements within a prerelease and build`() {
        val version = Version.fromString("1.2.3-a.b.c+d.e.f")
        "a.b.c".split(".").forEachIndexed{i, it -> assertEquals(it, version.preReleaseElements[i]) }
        "d.e.f".split(".").forEachIndexed{i, it ->  assertEquals(it, version.metadataElements[i]) }
    }
}