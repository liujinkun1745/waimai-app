package com.waimai.service;

import com.waimai.entity.*;
import com.waimai.exception.BusinessException;
import com.waimai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final BalanceRecordRepository balanceRecordRepository;

    @Transactional
    public Order submitOrder(Long consumerId, Long merchantId, Long addressId,
                              List<CartItem> cartItems, BigDecimal totalAmount) {
        User consumer = userRepository.findById(consumerId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (consumer.getBalance().compareTo(totalAmount) < 0) {
            throw new BusinessException("余额不足，请先充值");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("地址不存在"));

        String orderNo = "WM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6);

        Merchant merchantRef = merchantRepository.getReferenceById(merchantId);
        Order order = Order.builder()
                .orderNo(orderNo)
                .consumer(consumer)
                .merchant(merchantRef)
                .addressSnapshot(buildAddressSnapshot(address))
                .totalAmount(totalAmount)
                .status("待接单")
                .paidAt(LocalDateTime.now())
                .build();
        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .productImage(item.getProductImage())
                    .price(item.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
            orderItemRepository.save(orderItem);
        }

        consumer.setBalance(consumer.getBalance().subtract(totalAmount));
        userRepository.save(consumer);

        BalanceRecord record = BalanceRecord.builder()
                .user(consumer)
                .amount(totalAmount.negate())
                .type("消费")
                .description("订单支付: " + orderNo)
                .build();
        balanceRecordRepository.save(record);

        return order;
    }

    @Transactional
    public void cancelByConsumer(Long orderId, Long consumerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getConsumer().getId().equals(consumerId)) {
            throw new BusinessException("无权操作");
        }
        if (!"待付款".equals(order.getStatus()) && !"待接单".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }
        boolean wasPaid = !"待付款".equals(order.getStatus());
        order.setStatus("已取消");
        orderRepository.save(order);

        if (wasPaid) {
            User consumer = order.getConsumer();
            consumer.setBalance(consumer.getBalance().add(order.getTotalAmount()));
            userRepository.save(consumer);

            BalanceRecord record = BalanceRecord.builder()
                    .user(consumer)
                    .amount(order.getTotalAmount())
                    .type("充值")
                    .description("订单退款: " + order.getOrderNo())
                    .build();
            balanceRecordRepository.save(record);
        }
    }

    @Transactional
    public void acceptOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException("无权操作");
        }
        if (!"待接单".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可接单");
        }
        order.setStatus("待配送");
        order.setAcceptedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void startDelivery(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException("无权操作");
        }
        if (!"待配送".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可配送");
        }
        order.setStatus("配送中");
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void completeOrder(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new BusinessException("无权操作");
        }
        if (!"配送中".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可完成");
        }
        order.setStatus("已完成");
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        Merchant m = order.getMerchant();
        m.setMonthlySales(m.getMonthlySales() + 1);
        merchantRepository.save(m);
    }

    @Transactional
    public void confirmReceived(Long orderId, Long consumerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getConsumer().getId().equals(consumerId)) {
            throw new BusinessException("无权操作");
        }
        if (!"配送中".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可确认收货");
        }
        order.setStatus("已完成");
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);
        Merchant m = order.getMerchant();
        m.setMonthlySales(m.getMonthlySales() + 1);
        merchantRepository.save(m);
    }

    public List<Order> listByConsumer(Long consumerId, String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findByConsumerIdOrderByCreatedAtDesc(consumerId);
        }
        return orderRepository.findByConsumerIdAndStatus(consumerId, status);
    }

    public List<Order> listByMerchant(Long merchantId, String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        }
        return orderRepository.findByMerchantIdAndStatus(merchantId, status);
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Map<Long, List<OrderItem>> getOrderItemsBatch(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Map.of();
        List<OrderItem> all = orderItemRepository.findByOrderIdIn(orderIds);
        return all.stream().collect(Collectors.groupingBy(
                item -> item.getOrder().getId()));
    }

    private String buildAddressSnapshot(Address a) {
        return (a.getProvince() != null ? a.getProvince() : "") +
                (a.getCity() != null ? a.getCity() : "") +
                (a.getDistrict() != null ? a.getDistrict() : "") +
                a.getDetailAddress() + " | " + a.getReceiverName() + " " + a.getReceiverPhone();
    }

    public static class CartItem {
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        public CartItem() {}
        public CartItem(Long productId, String productName, String productImage,
                         BigDecimal price, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductImage() { return productImage; }
        public void setProductImage(String productImage) { this.productImage = productImage; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }
}
