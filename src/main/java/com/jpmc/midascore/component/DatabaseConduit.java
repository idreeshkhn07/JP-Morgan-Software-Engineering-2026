package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseConduit {

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveClient incentiveClient;

    public DatabaseConduit(UserRepository userRepository,
                           TransactionRecordRepository transactionRecordRepository,
                           IncentiveClient incentiveClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveClient = incentiveClient;
    }

    // Used by UserPopulator in tests to seed the database
    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    /**
     * Task 4 flow:
     * - validate sender/recipient exist
     * - validate sender has enough balance
     * - call incentive API (after validation)
     * - update balances (recipient gets amount + incentive; sender loses only amount)
     * - persist TransactionRecord including incentive
     */
    @Transactional
    public void process(Transaction tx) {

        long senderId = tx.getSenderId();
        long recipientId = tx.getRecipientId();
        float amount = tx.getAmount();

        var senderOpt = userRepository.findById(senderId);
        var recipientOpt = userRepository.findById(recipientId);

        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            return; // discard
        }

        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();

        if (sender.getBalance() < amount) {
            return; // discard
        }

        // Call incentives API only for valid transactions
        float incentive = incentiveClient.fetchIncentiveAmount(tx);

        // Update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentive);

        // Persist updated users
        userRepository.save(sender);
        userRepository.save(recipient);

        // Persist transaction record with incentive
        TransactionRecord record = new TransactionRecord(sender, recipient, amount, incentive);
        transactionRecordRepository.save(record);
    }
}