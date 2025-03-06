import { oc_orderRepository } from "../dao/oc_orderRepository";

export function onMessage(message: any) {
    const repository = new oc_orderRepository();
    const openCartOrders = repository.findAll();

    message.setBody(openCartOrders);

    const exchangeRate = 0.92;
    message.setExchangeProperty("currencyExchangeRate", exchangeRate);

    return message;
}
