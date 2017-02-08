package com.github.slack.cleaner;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.channels.ChannelsHistoryRequest;
import com.github.seratch.jslack.api.methods.request.channels.ChannelsListRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatDeleteRequest;
import com.github.seratch.jslack.api.methods.response.channels.ChannelsHistoryResponse;
import com.github.seratch.jslack.api.methods.response.channels.ChannelsListResponse;
import com.github.seratch.jslack.api.methods.response.chat.ChatDeleteResponse;
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
  }

  private void cleanChannel(final String token, final String channel) throws IOException, SlackApiException, InterruptedException
  {
    final Slack slack = Slack.getInstance();

    System.out.println(String.format("Retrieving messages for channel %s...", channel));

    final ChannelsListResponse channels = slack.methods().channelsList(ChannelsListRequest.builder()
                                                                                          .token(token)
                                                                                          .build());

    final Optional<Channel> match = channels.getChannels().stream().filter(c -> c.getName().equals(channel)).findFirst();
    if (!match.isPresent())
    {
      System.out.println(String.format("Channel %s not found.", channel));
    }
    else
    {
      final ChannelsHistoryResponse response = slack.methods().channelsHistory(ChannelsHistoryRequest.builder()
                                                                                                     .token(token)
                                                                                                     .channel(match.get().getId())
                                                                                                     .build());

      if ((response.getMessages() == null) || response.getMessages().isEmpty())
      {
        System.out.println(String.format("No messages found in channel %s.", channel));
      }
      else
      {
        for (final Message message : response.getMessages())
        {
          System.out.println(String.format("Deleting message %s...", message.getTs()));

          final ChatDeleteResponse chat = slack.methods().chatDelete(ChatDeleteRequest.builder()
                                                                               .token(token)
                                                                               .channel(match.get().getId())
                                                                               .ts(message.getTs())
                                                                               .build());

          // Slack has a rate limit of one operation per second for its API.
          Thread.sleep(1050);
        }

        System.out.println(String.format("Cleaned channel %s.", channel));
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
