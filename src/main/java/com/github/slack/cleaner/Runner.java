/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.slack.cleaner;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatDeleteRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.Message;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Deletes all chat messages from a <a href="https://www.slack.com">Slack</a>
 * <a href="https://get.slack.help/hc/en-us/articles/360017938993-What-is-a-channel-">Channel</a>.
 */
public class Runner
{
  private Options options;

  /**
   * <p>
   * Starts the process of deleting chat messages from a specified Slack
   * channel. The name of the channel to clean and a Slack API token must be
   * passed from the command-line.
   * </p>
   *
   * <p>
   * The channel name can be passed as {@code -channel [channel name]}, where
   * {@code [channel name]} is the human-readable name of the channel, as
   * visible in any of the Slack applications, for example {@code team},
   * {@code alarms}, etc.
   * </p>
   *
   * <p>
   * The Slack API token can be passed as {@code -token [API token]}, where
   * {@code [API token]} is a Slack API token associated with the Slack account
   * that owns the channel from which chat messages should be deleted.
   * </p>
   *
   * @param args Command-line arguments passed to the program.
   * @throws ParseException       if the command-line arguments fail to parse
   *                              correctly.
   * @throws IOException          if there is any error in sending data to the
   *                              Slack API or receiving reply from the API.
   * @throws SlackApiException    if there is an error in invoking the Slack
   *                              API.
   * @throws InterruptedException if a Slack API call is interrupted before it
   *                              is complete.
   */
  public static void main(final String[] args) throws ParseException, IOException, SlackApiException, InterruptedException
  {
    new Runner().run(args);
  }

  /**
   * Starts the process of deleting chat messages from a specified Slack
   * channel. The name of the channel to clean and a Slack API token are read
   * from the command-line.
   *
   * @param args Command-line arguments passed to the program.
   * @throws ParseException       if the command-line arguments fail to parse
   *                              correctly.
   * @throws IOException          if there is any error in sending data to the
   *                              Slack API or receiving reply from the API.
   * @throws SlackApiException    if there is an error in invoking the Slack
   *                              API.
   * @throws InterruptedException if a Slack API call is interrupted before it
   *                              is complete.
   */
  private void run(final String[] args) throws ParseException, IOException, SlackApiException, InterruptedException
  {
    showBanner();

    // Parse the command-line arguments to read the Slack API token and the
    // name of the Slack channel to clean.
    final CommandLine commandLine = getCommandLine(args);

    if (!commandLine.hasOption(CommandLineOptions.CHANNEL)
        || !commandLine.hasOption(CommandLineOptions.TOKEN))
    {
      // If the channel name or API token has not been specified, display a
      // friendly help message to inform the user about how to run the utility.
      showHelp();
    }
    else
    {
      // Attempt to clean chat messages from the specified channel.
      cleanChannel(commandLine.getOptionValue(CommandLineOptions.TOKEN), commandLine.getOptionValue(CommandLineOptions.CHANNEL));
    }

    System.out.println("#                                                                              #");
    System.out.println("################################################################################");
  }

  /**
   * Deletes all chat messages from a specified Slack channel.
   *
   * @param token   The API token to use for communicating with the Slack API.
   * @param channel The human-readable name of the Slack channel to clean.
   * @throws IOException          if there is any error in sending data to the
   *                              Slack API or receiving reply from the API.
   * @throws SlackApiException    if there is an error in invoking the Slack
   *                              API.
   * @throws InterruptedException if a Slack API call is interrupted before it
   *                              is complete.
   */
  private void cleanChannel(final String token, final String channel) throws IOException, SlackApiException, InterruptedException
  {
    final Slack slack = Slack.getInstance();

    printLine(String.format("Retrieving messages for channel %s...", channel));

    // Retrieve a list of all channels for the account associated with the
    // provided API token.
    final ConversationsListResponse channels = slack.methods()
                                                    .conversationsList(ConversationsListRequest.builder()
                                                                                               .excludeArchived(true)
                                                                                               .token(token)
                                                                                               .types(Arrays.asList(ConversationType.PRIVATE_CHANNEL
                                                                                                   , ConversationType.PUBLIC_CHANNEL))
                                                                                               .build());

    if (channels.getError() != null)
    {
      // The channel list could not be retrieved so display an error.
      printLine("Unable to retrieve the list of channels for the specified token.");
      printLine("Please check and ensure that the token is valid and has the");
      printLine("necessary permissions to query channels for the account.");
    }
    else
    {
      // Attempt to find the specified channel name in the list of channels
      // received from the API.
      final Optional<Conversation> match = channels.getChannels()
                                                   .stream()
                                                   .filter(c -> c.getName().equals(channel)
                                                       && !c.isReadOnly())
                                                   .findFirst();
      if (!match.isPresent())
      {
        // The specified channel name is not present, so display an error
        // message.
        printLine(String.format("Channel %s not found.", channel));
      }
      else
      {
        // Attempt to find all chat messages for the specified channel.
        final ConversationsHistoryResponse response = slack.methods()
                                                           .conversationsHistory(ConversationsHistoryRequest.builder()
                                                                                                            .token(token)
                                                                                                            .channel(match.get().getId())
                                                                                                            .build());
        if (response.getError() != null)
        {
          // The channel list could not be retrieved so display an error.
          printLine(String.format("Unable to retrieve the list of messages for the channel %s.", channel));
          printLine("Please check and ensure that the token has the necessary permission");
          printLine("to query chat messages for the account.");
        }
        else if ((response.getMessages() == null) || response.getMessages().isEmpty())
        {
          // No messages found in the specified channel.
          printLine(String.format("No messages found in channel %s.", channel));
        }
        else
        {
          printLine(String.format("%d message(s) found in channel %s.", response.getMessages().size(), channel));

          for (final Message message : response.getMessages())
          {
            System.out.print(String.format("# Deleting message %s ... ", message.getTs()));

            try
            {
              slack.methods().chatDelete(ChatDeleteRequest.builder()
                                                          .token(token)
                                                          .channel(match.get().getId())
                                                          .ts(message.getTs())
                                                          .build());

              System.out.println(" deleted.");
            }
            catch (final Exception e)
            {
              printLine(String.format(" failed with error: %s", e.getMessage()));
            }

            // Slack has a rate limit of one operation per second for its API
            // so wait for a little over 1 second before sending the next API
            // request.
            Thread.sleep(1050);
          }

          printLine(String.format("Cleaned channel %s.", channel));
        }
      }
    }
  }

  /**
   * Parses the command-line arguments.
   *
   * @param args The command-line passed to the program.
   * @return A {@link CommandLine} containing command-line arguments read.
   * @throws ParseException if the command-line arguments fail to parse
   *                        correctly.
   */
  private CommandLine getCommandLine(final String[] args) throws ParseException
  {
    return new DefaultParser().parse(getOptions(), args);
  }

  /**
   * Provides the command-line options to be read.
   *
   * @return An {@link Options}.
   */
  private Options getOptions()
  {
    if (options == null)
    {
      options = new Options();

      options.addOption(Option.builder(CommandLineOptions.CHANNEL).hasArg().desc("Slack channel name").build());
      options.addOption(Option.builder(CommandLineOptions.TOKEN).hasArg().desc("Slack API token").build());
    }

    return options;
  }

  /**
   * Prints a line to the command-line.
   *
   * @param line The line to print.
   */
  private void printLine(final String line)
  {
    System.out.println(String.format("# %-76s #", line));
  }

  /**
   * Displays an information banner for the utility.
   */
  private void showBanner()
  {
    System.out.println("################################################################################");
    System.out.println("#                                                                              #");
    System.out.println("#         _____ _            _      _____ _                                    #");
    System.out.println("#        /  ___| |          | |    /  __ \\ |                                   #");
    System.out.println("#        \\ `--.| | __ _  ___| | __ | /  \\/ | ___  __ _ _ __   ___ _ __         #");
    System.out.println("#         `--. \\ |/ _` |/ __| |/ / | |   | |/ _ \\/ _` | '_ \\ / _ \\ '__|        #");
    System.out.println("#        /\\__/ / | (_| | (__|   <  | \\__/\\ |  __/ (_| | | | |  __/ |           #");
    System.out.println("#        \\____/|_|\\__,_|\\___|_|\\_\\  \\____/_|\\___|\\__,_|_| |_|\\___|_|           #");
    System.out.println("#                                                                              #");
    System.out.println("################################################################################");
    System.out.println("#                                                                              #");
  }

  /**
   * Displays help message for the utility to inform the user about correctly
   * usage of the utility..
   */
  private void showHelp()
  {
    new HelpFormatter().printHelp("slack-cleaner", getOptions());
  }

  /**
   * Contains names of arguments to be read from the command-line.
   */
  private static class CommandLineOptions
  {
    private static final String CHANNEL = "channel";
    private static final String TOKEN   = "token";
  }
}
