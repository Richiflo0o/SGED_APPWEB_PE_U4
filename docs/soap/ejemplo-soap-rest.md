# Ejemplo SOAP vs REST

Bloque B (Frontend + mediciones) — comparación entre una llamada a un
servicio **SOAP** público real y su equivalente conceptual **REST** dentro
de este proyecto (SGED).

## Servicio SOAP elegido

**Number Conversion Web Service** (dataaccess.com) — servicio público,
gratuito, sin autenticación, activo desde hace más de 15 años y usado
ampliamente en documentación y tutoriales de referencia sobre SOAP.

- WSDL: <https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL>
- Endpoint: `https://www.dataaccess.com/webservicesserver/NumberConversion.wso`
- Operación usada: `NumberToWords` — recibe un número entero y devuelve su
  representación en palabras (en inglés).

Se eligió este servicio (en vez de uno relacionado a fútbol/educación) por
ser el único de la lista de servicios SOAP públicos "clásicos" que sigue
respondiendo hoy sin cambios; la mayoría de los demás (CountryInfoService,
servicios de clima, calculadoras de moneda) llevan años dados de baja, lo
que los vuelve poco confiables como evidencia reproducible para este
informe.

## Petición SOAP real

```http
POST /webservicesserver/NumberConversion.wso HTTP/1.1
Host: www.dataaccess.com
Content-Type: text/xml; charset=utf-8
SOAPAction: "http://www.dataaccess.com/webservicesserver/NumberToWords"

<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>25</ubiNum>
    </NumberToWords>
  </soap:Body>
</soap:Envelope>
```

## Cómo capturar la respuesta real (para la evidencia del informe)

La documentación pública del servicio solo muestra una plantilla genérica
de respuesta (`string` como placeholder), no una captura real. Para tener
evidencia auténtica en el informe, corre esto tú mismo y pega la salida
real que te devuelva:

```bash
curl -s -X POST "https://www.dataaccess.com/webservicesserver/NumberConversion.wso" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H 'SOAPAction: "http://www.dataaccess.com/webservicesserver/NumberToWords"' \
  -d '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>25</ubiNum>
    </NumberToWords>
  </soap:Body>
</soap:Envelope>'
```

Debería devolverte un XML con `<NumberToWordsResult>` conteniendo la
representación en palabras de "25" (en inglés, ya que es un servicio
estadounidense). Pega esa salida real aquí abajo, reemplazando este
párrafo, antes de entregar el informe.

**[PENDIENTE: pegar aquí la respuesta XML real obtenida con el curl de arriba]**

## Endpoint REST equivalente del proyecto (PFC)

Del backend de este proyecto (`EstudianteController`), un endpoint con la
misma forma conceptual: recibe un identificador simple y devuelve un único
valor numérico.

```
GET /api/estudiantes/conteo/categoria/{idCategoria}
Cookie: sged_access=<jwt>
```

Ejemplo con la categoría de id `1`:

```http
GET /api/estudiantes/conteo/categoria/1 HTTP/1.1
Host: localhost:8080
Cookie: sged_access=eyJhbGciOi...
```

Respuesta:

```http
HTTP/1.1 200 OK
Content-Type: application/json

7
```

(El número de ejemplo es ilustrativo; el valor real depende de cuántos
estudiantes activos existan en esa categoría en la base de datos al
momento de la consulta.)

## Comparación

| Aspecto | SOAP (Number Conversion) | REST (SGED) |
|---|---|---|
| Formato del mensaje | XML rígido, envuelto en `soap:Envelope` | JSON plano, sin envoltorio |
| Contrato | WSDL (contract-first, formal, generado aparte) | Documentado vía Swagger/OpenAPI, implícito en la URL + verbo HTTP |
| Verbo HTTP | Siempre `POST`, aunque la operación sea de solo lectura | `GET` semánticamente correcto para una consulta |
| Identificación de la operación | Va dentro del cuerpo XML y en la cabecera `SOAPAction` | Va en la URL (`/conteo/categoria/{id}`) |
| Autenticación | Ninguna (servicio de demostración público) | JWT en cookie `HttpOnly` (`sged_access`), validado en cada request |
| Tamaño típico de la respuesta | Mayor, por las etiquetas XML repetidas del envelope | Menor, un valor JSON directo |
| Cacheable por HTTP estándar | No (todo es `POST`) | Sí, en principio (`GET` es cacheable; en este proyecto además se usa caché en Redis del lado del servidor) |
| Estado del ecosistema | Legado; la mayoría de servicios SOAP públicos de referencia ya fueron dados de baja | Estándar de facto para APIs nuevas |

## Conclusión breve

La comparación confirma en la práctica lo que dice la teoría: SOAP exige
un contrato XML más pesado y verboso, fuerza todo a través de `POST`, y
depende de un WSDL formal para que el cliente sepa qué operaciones existen
y qué forma tienen sus parámetros. REST, en cambio, aprovecha directamente
la semántica de HTTP (verbos, códigos de estado, cacheabilidad) y entrega
respuestas más compactas en JSON, lo que explica por qué es la opción
elegida para la API de este proyecto (ver ADR-001 y ADR-002).