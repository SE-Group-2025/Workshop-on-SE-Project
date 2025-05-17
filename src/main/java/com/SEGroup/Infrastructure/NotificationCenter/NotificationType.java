package com.SEGroup.Infrastructure.NotificationCenter;

/** Every notification that flows to the UI must have one of these types. */
public enum NotificationType {
    /* --- regular offers -------------------------------------------------- */
    BID,                       // customer sent an offer
    BID_ACCEPTED,              // ALL owners approved
    BID_REJECTED,              // owner rejected
    BID_COUNTER,               // owner sent counter-offer
    BID_APPROVAL_NEEDED,       // another owner must approve
    BID_APPROVAL_OK,           // an owner has approved

    /* --- auctions -------------------------------------------------------- */
    AUCTION_BID,               // new highest bid
    AUCTION_OUTBID,            // somebody was just out-bid
    AUCTION_WIN                // auction finished – winner
}
