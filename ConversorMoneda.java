import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ConversorMoneda {

    // Si prefieres, puedes poner tu clave directamente aquí o usar variable de entorno.
    private static final String API_KEY = System.getenv("EXCHANGE_RATE_API_KEY") != null
            ? System.getenv("EXCHANGE_RATE_API_KEY")
            TU API KEY AQUI // <-- tu clave (mejor manejarla en variable de entorno)

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\nPor favor escoja una opción:");
            System.out.println("1. COP (Pesos colombianos)");
            System.out.println("2. EUR (Euro)");
            System.out.println("3. GBP (Libra esterlina)");
            System.out.println("4. MXN (Pesos mexicanos)");
            System.out.println("5. Salir");
            System.out.print("Opción: ");

            String opcion = sc.nextLine().trim();

            String monedaBase;
            switch (opcion) {
                case "1" -> monedaBase = "COP";
                case "2" -> monedaBase = "EUR";
                case "3" -> monedaBase = "GBP";
                case "4" -> monedaBase = "MXN";
                case "5" -> {
                    System.out.println("Saliendo...");
                    sc.close();
                    return;
                }
                default -> {
                    System.out.println("Opción no válida. Intenta de nuevo.");
                    continue;
                }
            }

            System.out.print("Ingrese la cantidad en " + monedaBase + ": ");
            String cantidadStr = sc.nextLine().trim();
            double cantidad;
            try {
                cantidad = Double.parseDouble(cantidadStr.replace(",", ""));
                if (cantidad < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("Cantidad inválida. Debe ser un número positivo.");
                continue;
            }

            try {
                double equivalenteUsd = convertirADolares(monedaBase, cantidad);
                if (equivalenteUsd < 0) {
                    System.out.println("No se pudo obtener la tasa de cambio. Intenta más tarde.");
                } else {
                    BigDecimal original = BigDecimal.valueOf(cantidad).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal convertido = BigDecimal.valueOf(equivalenteUsd).setScale(2, RoundingMode.HALF_UP);
                    System.out.printf("%s %s equivalen a %s USD%n", original.toPlainString(), monedaBase, convertido.toPlainString());
                }
            } catch (Exception e) {
                System.out.println("Error durante la conversión: " + e.getMessage());
            }
        }
    }

    private static double convertirADolares(String baseCurrency, double amount) throws Exception {
        if (baseCurrency.equalsIgnoreCase("USD")) {
            return amount; // ya está en dólares
        }

        String url = String.format("https://v6.exchangerate-api.com/v6/%s/latest/%s", API_KEY, baseCurrency);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Respuesta no exitosa del servidor: " + resp.statusCode());
        }

        JsonObject root = gson.fromJson(resp.body(), JsonObject.class);
        String result = root.has("result") ? root.get("result").getAsString() : "";
        if (!"success".equalsIgnoreCase(result)) {
            throw new RuntimeException("La API respondió con un estado inesperado: " + result);
        }

        JsonObject rates = root.getAsJsonObject("conversion_rates");
        if (rates == null || !rates.has("USD")) {
            throw new RuntimeException("No se encontró la tasa de USD en la respuesta.");
        }

        double tasaUsdPorUnidadBase = rates.get("USD").getAsDouble(); // 1 baseCurrency = X USD
        return amount * tasaUsdPorUnidadBase;
    }
}
