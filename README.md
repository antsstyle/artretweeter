# ArtRetweeter
A tool for artists to use that allows you to automatically schedule retweets of your art (or manually if you so choose), and enables use of the Twitter Collections feature.

## Downloading and using ArtRetweeter

If you're using 64-bit Windows, you can download the "with-Java-for-Win64" zip from the latest release (see the right side of this page for the releases tab). Simply unzip and run the EXE.

If you're using another operating system, download the zip without Java bundled, unzip it and run the jar file. You will need to install Java before you can run it (https://jdk.java.net/)

## What are Twitter Collections?

Twitter Collections are a feature for listing tweets of your choice (rather than the Lists feature, which only allows you to put users in a list but not control which tweets are in the list).

Collections support three ordering types: tweets sorted by newest-first chronologically, oldest-first chronologically, or reverse curation order (i.e. the tweets are in the order you add them, with the latest one you've added at the top of the list).

Here is an example collection: https://twitter.com/antsstyle/timelines/1410230429718695941

ArtRetweeter doesn't currently support changing the order of tweets in a reverse curation order collection - this will be implemented in a future version.

## Known Bugs

- Some users may get incorrect "Date Posted" values in the Tweet Management table, usually on or around 1970-01-01. This bug does not impact any functionality.

## Tech Stuff

The server-side part of ArtRetweeter is written in PHP and uses TwitterOAuth (https://twitteroauth.com/).

The client application is written in Java.
