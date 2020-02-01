# Simple Makefile wrapper for Gradle for LabTS integration

GRADLE := ./gradlew

all:
	$(GRADLE) build

test:
	$(GRADLE) test

clean:
	$(GRADLE) clean

.PHONY: all clean test
