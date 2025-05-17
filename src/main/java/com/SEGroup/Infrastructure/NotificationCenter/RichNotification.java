package com.SEGroup.Infrastructure.NotificationCenter;

public class RichNotification extends NotificationWithSender {

    private final NotificationType type;
    private final double           price;      // 0⇢not relevant
    private final String           productId;  // null⇢not relevant
    private final String           extra;      // free text (optional)

    public RichNotification(String receiverId,
                            String senderId,
                            String message,
                            NotificationType type,
                            double price,
                            String productId,
                            String extra)
    {
        super(message, receiverId, senderId);
        this.type      = type;
        this.price     = price;
        this.productId = productId;
        this.extra     = extra;
    }

    /* getters ----------------------------------------------------------- */

    public NotificationType getType() { return type; }
    public double           getPrice()     { return price; }
    public String           getProductId() { return productId; }
    public String           getExtra()     { return extra; }

    @Override public String toString() {
        return "RichNotification{" + getMessage() + " " + type + " $" + price +
                " product=" + productId + " extra=" + extra + '}';
    }
}
