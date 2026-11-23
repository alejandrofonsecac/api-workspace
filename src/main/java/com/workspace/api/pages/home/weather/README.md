# Clima

Retorna a temperatura, a condição do tempo e o nome da cidade para uma coordenada.
A API consulta serviços externos gratuitos, sem chave de acesso.

## Como funciona

1. Valida a coordenada: latitude entre -90 e 90, longitude entre -180 e 180.
2. Procura no cache em memória. A chave é a coordenada arredondada para duas casas
   (cerca de 1 km), válida por 10 minutos, com até 1.000 entradas por instância.
3. Consulta a **Open-Meteo**. Se ela falhar ou responder dados incompletos, usa o
   **MET Norway**. Quando a Open-Meteo responde `429` (cota esgotada), ela é ignorada
   por 1 hora e as consultas vão direto ao MET Norway.
4. Consulta o **Nominatim** (OpenStreetMap) para o nome da cidade. Se falhar, `city` é
   `null` e o clima é retornado mesmo assim.
5. Converte o código do provedor em um texto em português, como `Céu limpo`, `Chuva` ou
   `Trovoada`; códigos desconhecidos viram `Condição desconhecida`.

Cada chamada externa tem tempo limite de 3 s para conectar e 4 s para ler
(`config/RestClientConfig`). MET Norway e Nominatim recebem um `User-Agent` que
identifica a aplicação, como exigem seus termos de uso.

## Endpoint

- `GET /weather?latitude=-23.55&longitude=-46.63` (autenticado)

```json
{ "temperature": 24.3, "city": "São Paulo", "condition": "Parcialmente nublado" }
```

## Erros

- `400`: coordenada fora da faixa ou ausente.
- `502`: Open-Meteo e MET Norway falharam.

## Código

- `controller/WeatherController`, `service/WeatherService` (cache, provedores, conversão).
- `validation/WeatherValidator`: coordenadas e respostas dos provedores.
- `dto/`: formatos da Open-Meteo, MET Norway, Nominatim e da resposta da API.

## Testes

- `WeatherControllerTest`, `WeatherServiceTest`, `WeatherValidatorTest`.
