package org.nextgate.nextgatebackend.financial_system.transaction_history.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.financial_system.transaction_history.entity.TransactionHistory;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.financial_system.transaction_history.payload.TransactionHistoryResponse;
import org.nextgate.nextgatebackend.financial_system.transaction_history.service.TransactionHistoryService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transaction-history")
@RequiredArgsConstructor
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionHistory> transactions = transactionHistoryService.getMyTransactions(pageable);
        Page<TransactionHistoryResponse> response = transactions.map(TransactionHistoryResponse::fromEntity);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transactions retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getTransactionById(@PathVariable UUID id) throws ItemNotFoundException {

        TransactionHistory transaction = transactionHistoryService.getById(id);
        TransactionHistoryResponse response = TransactionHistoryResponse.fromEntity(transaction);


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transaction retrieved successfully", response)
        );
    }

    @GetMapping("/ref/{transactionRef}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getTransactionByRef(@PathVariable String transactionRef)
            throws ItemNotFoundException {

        TransactionHistory transaction = transactionHistoryService.getByTransactionRef(transactionRef);
        TransactionHistoryResponse response = TransactionHistoryResponse.fromEntity(transaction);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transaction retrieved successfully", response));
    }

    @GetMapping("/filter/type")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyTransactionsByType(
            @RequestParam TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionHistory> transactions = transactionHistoryService.getMyTransactionsByType(type, pageable);
        Page<TransactionHistoryResponse> response = transactions.map(TransactionHistoryResponse::fromEntity);


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transactions retrieved successfully", response)
        );
    }

    @GetMapping("/filter/direction")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyTransactionsByDirection(
            @RequestParam TransactionDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionHistory> transactions = transactionHistoryService.getMyTransactionsByDirection(direction, pageable);
        Page<TransactionHistoryResponse> response = transactions.map(TransactionHistoryResponse::fromEntity);


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transactions retrieved successfully", response)
        );
    }

    @GetMapping("/filter/date-range")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws ItemNotFoundException {

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionHistory> transactions = transactionHistoryService.getMyTransactionsByDateRange(
                startDate, endDate, pageable);
        Page<TransactionHistoryResponse> response = transactions.map(TransactionHistoryResponse::fromEntity);


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transactions retrieved successfully", response)
        );
    }

    @GetMapping("/count")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyTransactionCount() throws ItemNotFoundException {

        long count = transactionHistoryService.getMyTransactionCount();


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Transaction count retrieved successfully", count)
        );
    }
}