package com.github.slack.cleaner;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.channels.ChannelsHistoryRequest;
import com.github.seratch.jslack.api.methods.request.channels.ChannelsListRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatDeleteRequest;
import com.github.seratch.jslack.api.methods.response.channels.ChannelsHistoryResponse;
import com.github.seratch.jslack.api.methods.response.channels.ChannelsListResponse;
import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Message;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Optional;

public class Runner
{
  private Options options;

  public static void main(final String[] args) throws ParseException, IOException, SlackApiException, InterruptedException
  {
    new Runner().run(args);
  }

  private void run(final String[] args) throws ParseException, IOException, SlackApiException, InterruptedException
  {
    showBanner();

    final CommandLine commandLine = getCommandLine(args);

    if (!commandLine.hasOption(CommandLineOptions.CHANNEL)
        || !commandLine.hasOption(CommandLineOptions.TOKEN))
    {
      showHelp();
    }
    else
    {
      cleanChannel(commandLine.getOptionValue(CommandLineOptions.TOKEN), commandLine.getOptionValue(CommandLineOptions.CHANNEL));
    }

    System.out.println("#                                                                                #");
    System.out.println("##################################################################################");
  }

  private void cleanChannel(final String token, final String channel) throws IOException, SlackApiException, InterruptedException
  {
    final Slack slack = Slack.getInstance();

    System.out.print(String.format("# Retrieving messages for channel %s...", channel));

    final ChannelsListResponse channels = slack.methods().channelsList(ChannelsListRequest.builder()
                                                                                          .token(token)
                                                                                          .build());

    final Optional<Channel> match = channels.getChannels().stream().filter(c -> c.getName().equals(channel)).findFirst();
    if (!match.isPresent())
    {
      System.out.println(String.format("# Channel %s not found.", channel));
    }
    else
    {
      final ChannelsHistoryResponse response = slack.methods().channelsHistory(ChannelsHistoryRequest.builder()
                                                                                                     .token(token)
                                                                                                     .channel(match.get().getId())
                                                                                                     .build());

      if ((response.getMessages() == null) || response.getMessages().isEmpty())
      {
        System.out.println(String.format("# No messages found in channel %s.", channel));
      }
      else
      {
        System.out.println(String.format("# %d messages found in channel %s.", response.getMessages().size(), channel));
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
            System.out.println(String.format(" failed with error: %s", e.getMessage()));
          }

          // Slack has a rate limit of one operation per second for its API
          // so wait for a little over 1 second before sending the next API
          // request.
          Thread.sleep(1050);
        }

        System.out.println(String.format("# Cleaned channel %s.", channel));
      }
    }

  }

  private CommandLine getCommandLine(final String[] args) throws ParseException
  {
    return new DefaultParser().parse(getOptions(), args);
  }

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

  private void showBanner()
  {
    System.out.println("##################################################################################");
    System.out.println("#                                                                                #");
    System.out.println("#          _____ _            _      _____ _                                     #");
    System.out.println("#         /  ___| |          | |    /  __ \\ |                                    #");
    System.out.println("#         \\ `--.| | __ _  ___| | __ | /  \\/ | ___  __ _ _ __   ___ _ __          #");
    System.out.println("#          `--. \\ |/ _` |/ __| |/ / | |   | |/ _ \\/ _` | '_ \\ / _ \\ '__|         #");
    System.out.println("#         /\\__/ / | (_| | (__|   <  | \\__/\\ |  __/ (_| | | | |  __/ |            #");
    System.out.println("#         \\____/|_|\\__,_|\\___|_|\\_\\  \\____/_|\\___|\\__,_|_| |_|\\___|_|            #");
    System.out.println("#                                                                                #");
    System.out.println("##################################################################################");
    System.out.println("#                                                                                #");
  }

  private void showHelp()
  {
    new HelpFormatter().printHelp("slack-cleaner", getOptions());
  }

  private static class CommandLineOptions
  {
    private static final String CHANNEL = "channel";
    private static final String TOKEN   = "token";
  }
}
