import { oc_orderRepository } from "orders-etl/dao/oc_orderRepository";
import { client } from "sdk/http";

export function onMessage(message: any) {
    const repository = new oc_orderRepository();
    const openCartOrders = repository.findAll();
    message.setBody(openCartOrders);

    const exchangeRate = getUsdToEuroExchangeRate();
    message.setExchangeProperty("currencyExchangeRate", exchangeRate);

    return message;
}

function getUsdToEuroExchangeRate() {
    const httpResponse = client.get("https://api.frankfurter.app/latest?from=USD&to=EUR");
    if (httpResponse.statusCode != 200) {
        const errorMessage = `Received unexpected response: ${JSON.stringify(httpResponse)}`;
        throw new Error(errorMessage);
    }

    const responseBody = JSON.parse(httpResponse.text);
    return responseBody.rates.EUR;
}