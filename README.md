# ArtRetweeter
A tool for artists to use, focused on the Twitter collections feature and retweeting previous posts.

The server-side part is written in PHP and uses TwitterOAuth (https://twitteroauth.com/).

The client application is written in Java and uses the Java Runtime Environment.

## Downloading and using ArtRetweeter

If you're using 64-bit Windows, you can download ArtRetweeter-v2.0.1-with-Java-for-Win-64.zip from the latest release (see the right side of this page for the releases tab, or direct link here). Simply unzip and run ArtRetweeter v2.0.1.exe.

If you're using another operating system, download ArtRetweeter-v2.0.1.zip, unzip it and run ArtRetweeter v2.0.jar. You will need to install Java before you can run it (https://jdk.java.net/16/)

## What are Twitter Collections?

Twitter Collections are a feature for listing tweets of your choice (rather than the Lists feature, which only allows you to put users in a list but not control which tweets are in the list).

Collections support three ordering types: tweets sorted by newest-first chronologically, oldest-first chronologically, or reverse curation order (i.e. the tweets are in the order you add them, with the latest one you've added at the top of the list).

Here is an example collection: https://twitter.com/antsstyle/timelines/1410230429718695941

ArtRetweeter doesn't currently support changing the order of tweets in a reverse curation order collection - this will be implemented in a future version.
