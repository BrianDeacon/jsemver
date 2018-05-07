# JSemver
### A Semver and version range parser for the JVM

JSemver parses strings conforming to the semver specification defined at [https://semver.org](https://semver.org).
It also validates those versions against version requirement strings following the standards for:
* Maven/Ivy
* NPM
* CocoaPods
* Ruby Gems

To check if a version satisfies a requirement:
```
assertTrue(Version.fromString("1.1.0").satisfies("[1.0,2.0]", RequirementType.MAVEN));
assertTrue(VersionRequirement.fromString("1.0.0-2.0.0").isSatisfiedBy("1.0.0"));
assertTrue(VersionRequirement.fromString("^1.2.3", RequirementType.NPM).isSatisfiedBy("1.2.5"));
assertTrue(NpmVersionRequirement,fromString(">1.2.3 <=3.1.4").isSatisfiedBy("2.0.0"));
assertTrue(VersionRequirement.fromString("~> 1.2", RequirementType.RUBY).isSatisfiedBy("1.2.3"));
```

The various parts of semver strings can be extracted:
```
assertEquals("123", Version("1.0.0-alpha+123").getBuild());
assertEquals("alpha", Version("1.0.0-alpha+123").getPreRelease());
```

And compared:
```
assertTrue(new Version("1.0.0").isGreaterThan(Version.fromString("0.9.0")));
assertTrue(new Version("1.0.0-SNAPSHOT").isLessThan(Version.fromString("1.0.0")));

//According to the semver specification, build metadata does not factor into versioning:
assertTrue(new Version("1.0.0-alpha+build1").isEquivalentTo(new Version("1.0.0-alpha+build2")));

```
Currently, JSemver supports simple version ranges ("1.0.0-2.0.0") as well as:
* [Maven version range format](https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html)
* [NPM semver format](https://www.npmjs.com/package/semver)
* [CocoaPods](https://guides.cocoapods.org/using/the-podfile.html)
* [Ruby Gems](http://guides.rubygems.org/patterns/#semantic-versioning) 

JSemver is written in Kotlin, but is designed to be java-friendly and is compatible with any JVM language
using 1.6 or greater.


