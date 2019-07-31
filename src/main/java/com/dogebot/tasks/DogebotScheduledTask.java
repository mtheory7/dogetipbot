package com.dogebot.tasks;

import com.dogebot.domain.DogeWallet;
import com.dogebot.repository.DogeWalletRepository;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DogebotScheduledTask {

  private static final Logger logger = Logger.getLogger(DogebotScheduledTask.class);
  private final String TWITTER_ID = "1154058021426872321";
  private final DogeWalletRepository repository;
  private String consumerKey = "";
  private String consumerSecret = "";
  private String accessToken = "";
  private String accessTokenSecret = "";
  private List<String> FUNCTIONS = Arrays.asList("+register", "+info", "+balance");

  @Autowired
  public DogebotScheduledTask(DogeWalletRepository repository) {
    this.repository = repository;
  }

  // @Scheduled(fixedDelay = 60000)
  public void handleTipMentions() {
    try {
      ConfigurationBuilder cb = new ConfigurationBuilder();
      cb.setDebugEnabled(true)
          .setOAuthConsumerKey(consumerKey)
          .setOAuthConsumerSecret(consumerSecret)
          .setOAuthAccessToken(accessToken)
          .setOAuthAccessTokenSecret(accessTokenSecret);
      TwitterFactory tf = new TwitterFactory(cb.build());

      for (int i = 1; i < 5; i++) {
        Paging paging = new Paging(i, 20);
        ResponseList<Status> mentions = tf.getInstance().getMentionsTimeline(paging);
        for (Status mention : mentions) {
          if (!mention.isFavorited()) {
            Double tipAmount = 0.0;
            DogeWallet tipper = null;
            DogeWallet tippee = null;
            // Start verifying doge tip
            // - Must have exactly 2 x @userMentions , @muchtipsuchwow and @userToBeTipped
            // Finding tippee (receiver of tip)
            if (mention.getUserMentionEntities().length == 2) {
              for (UserMentionEntity user : mention.getUserMentionEntities()) {
                if (user.getId() != Long.parseLong(TWITTER_ID)) {
                  List<DogeWallet> tippeeList =
                      repository.findByTwitterUserId(String.valueOf(user.getId()));
                  if (tippeeList.size() == 1) {
                    tippee = tippeeList.get(0);
                  } else {
                    // TODO Twitter tippee does not have a doge wallet yet OR has duplicates
                  }
                }
              }
            } else {
              // TODO Report incorrect tip format, expected exactly 2 user mention entities
            }
            // Finding tipper (sender of tip)
            List<DogeWallet> tipperList =
                repository.findByTwitterUserId(String.valueOf(mention.getUser().getId()));
            if (tipperList.size() == 1) {
              tipper = tipperList.get(0);
            } else {
              // TODO Twitter tipper does not have a doge wallet yet OR has duplicates
            }
            // Finding tip amount
            String[] strArray = mention.getText().split(" ");
            for (String str : strArray) {
              if (NumberUtils.isParsable(str)) {
                tipAmount = Double.parseDouble(str);
              }
            }

            if (tipper != null && tippee != null && tipAmount > 0) {}

            // - Must have only 1 number with no text in/around it (tip amount)
            //
            // List<DogeWallet> tippee = repository.findByTwitterUserId(String.valueOf())
          }
        }
      }
    } catch (Exception e) {
    }
  }

  @Scheduled(fixedDelay = 60000)
  public void handleDMInbox() {
    logger.info("Starting to execute scheduled task...");
    try {
      String cursor = "NONE";
      while (!cursor.equals("")) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(consumerKey)
            .setOAuthConsumerSecret(consumerSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        DirectMessageList dms =
            cursor.equals("NONE")
                ? tf.getInstance().getDirectMessages(20)
                : tf.getInstance().getDirectMessages(20, cursor);
        logger.info(
            "Number of messages returned: "
                + dms.size()
                + " with request of size 20 using cursor: "
                + cursor);
        cursor = dms.getNextCursor() == null ? "" : dms.getNextCursor();
        if (dms.isEmpty()) {
          cursor = "";
        }
        logger.info("===> new cursor = \"" + cursor + "\"");
        List<DirectMessage> dmsToAddress = new ArrayList<>();
        for (DirectMessage dm : dms) {
          if (dm.getSenderId() != Long.parseLong(TWITTER_ID)) {
            logger.info("Adding message to address: " + dm.getId() + " with text: " + dm.getText());
            dmsToAddress.add(dm);
          } else {
            logger.trace("Destroying DM with id: " + dm.getId());
            tf.getInstance().destroyDirectMessage(dm.getId());
          }
        }
        if (!dmsToAddress.isEmpty()) {
          for (DirectMessage dm : dmsToAddress) {
            logger.trace("Addressing tweet " + dm.getId());
            if (!FUNCTIONS.contains(dm.getText())) {
              // Not even an available function so delete it and continue
              logger.trace("Message is not a valid function " + dm.getId());
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
              continue;
            }
            List<DogeWallet> usersWallet =
                repository.findByTwitterUserId(String.valueOf(dm.getSenderId()));
            if (usersWallet.size() > 1) {
              tf.getInstance()
                      .sendDirectMessage(
                              dm.getSenderId(),
                              "For some strange reason, you have two (or more) accounts. Fix this by messaging @mtheoryStuhlman about your issue.");
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
              continue;
            }
            if (usersWallet.size() == 0 && !dm.getText().equals("+register")) {
              tf.getInstance()
                  .sendDirectMessage(
                      dm.getSenderId(),
                      "You have yet to create an account. Do this by sending a message containing:\n+register");
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
              continue;
            }
            if (dm.getText().equals("+register")) {
              DogeWallet wallet = new DogeWallet(String.valueOf(dm.getSenderId()));
              logger.info("New account created!");
              repository.save(wallet);
              tf.getInstance()
                  .sendDirectMessage(
                      dm.getSenderId(), "New account created!\n" + wallet.toString());
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
              continue;
            }
            if (dm.getText().equals("+info")) {
              tf.getInstance()
                  .sendDirectMessage(
                      dm.getSenderId(),
                      "Here is your account information\n" + usersWallet.get(0).toString());
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
            }
            if (dm.getText().equals("+balance")) {
              tf.getInstance()
                      .sendDirectMessage(
                              dm.getSenderId(),
                              "Balance: " + usersWallet.get(0).getBalance() + " D");
              logger.trace("Destroying DM with id: " + dm.getId());
              tf.getInstance().destroyDirectMessage(dm.getId());
            }
          }
        }
        logger.trace("Waiting 60 seconds to let API cool down...");
        Thread.sleep(60000);
      }
    } catch (Exception e) {
    }
  }
}
