# slack-cleaner

Java utility to delete all messages from a [Slack](https://www.slack.com) channel.

### Install

    mvn clean package

This will generate a JAR file in the `target` folder.

### Before running

Make sure to have an OAuth token for your [Slack team](https://api.slack.com/docs/oauth-test-tokens).

### Usage

Run the JAR as

    java -jar slack-cleaner*.jar -token [Slack API token] -channel [Slack channel name]

Replace `[Slack API token]` with the Slack OAuth token for your team and `[Slack channel name]` with the readable channel name from which messages need to be deleted (for example, `general`, or `team`).

# License
This application and its associated source code in its entirety is being made
available under the following licensing terms.

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in the
    Software without restriction, including without limitation the rights to use, copy,
    modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the
    following conditions:

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
    INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
    PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
    HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
    CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
    OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
