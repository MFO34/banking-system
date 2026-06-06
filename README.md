# Banking System — Mikroservis Projesi

Banka teknik mülakatına hazırlık amacıyla sıfırdan inşa ettiğim mikroservis sistemi. Her kavramı elle kurarak içselleştirmeyi hedefledim.

## Mimari

```
Client → API Gateway (JWT Auth, Load Balancer) :9090
              ↓                    ↓
    account-service :8080    transfer-service :8081
    PostgreSQL :5432          PostgreSQL :5433
    Redis Cache               Kafka Outbox
    Kafka Consumer            Circuit Breaker
                                   ↓
                           fraud-service :8082
```

## Servisler

### account-service (Port 8080)
Hesap yönetimi. Bakiye debit/credit işlemleri pessimistic locking ile korunuyor.
- `POST /accounts` — hesap oluştur
- `GET /accounts/{id}` — hesap getir (Redis cache)
- `POST /accounts/{id}/debit` — para çek (SELECT FOR UPDATE)
- `POST /accounts/{id}/credit` — para yatır (SELECT FOR UPDATE)

### transfer-service (Port 8081)
Para transferi. Saga pattern ile tutarlılık, Outbox pattern ile güvenilir event yayını.
- `POST /transfers` — transfer başlat (Idempotency-Key header zorunlu)

### api-gateway (Port 9090)
JWT doğrulama ve round-robin load balancing.
- `POST /auth/token` — token al (admin/admin)
- Diğer tüm istekler JWT ile korunuyor

### fraud-service (Port 8082)
Transfer öncesi fraud analizi.
- 10.000 TL üzeri işlemler reddedilir
- Yabancı para birimi reddedilir
- 1 dakikada 3+ transfer reddedilir

## Teknik Kararlar

**Neden Pessimistic Locking?**
Bankacılıkta iki eşzamanlı işlem aynı bakiyeyi okuyup güncelleyebilir (lost update). `SELECT FOR UPDATE` ile satır transaction boyunca kilitlenir. Optimistic locking'den farklı olarak retry gerekmez — bankacılıkta çakışma maliyeti yüksek.

**Neden Saga Pattern?**
`@Transactional` servisler arası çalışmaz. Her servis kendi DB'sine rollback yapar. Saga, yapılan işlemin tersini uygulayarak (compensating transaction) tutarlılığı sağlar.

**Neden Outbox Pattern?**
Transfer kaydedildi ama Kafka down olursa event kaybolur (dual write problemi). Outbox ile event aynı DB transaction'ında yazılır, poller Kafka'ya iletir. At-least-once garantisi sağlanır.

**Neden Idempotency Key?**
Ağ hatası durumunda client aynı isteği tekrar gönderebilir. Client'ın UUID ürettiği bu pattern ile aynı key'e sahip ikinci istek, DB'den cached response döner — para iki kez çıkmaz.

**Neden Circuit Breaker?**
account-service down olduğunda transfer-service thread'leri bloklanır. Yeterli istek geldiğinde tüm thread pool dolup cascading failure oluşur. Circuit Breaker, belirli hata eşiğinde istekleri anında reddeder.

**Neden Redis Cache?**
`GET /accounts/{id}` her çağrıda DB'ye gider. Sık okunan, az değişen veriler Redis'te tutulur. Debit/credit sonrası `@CacheEvict` ile cache temizlenir — stale bakiye gösterilmez.

## Öğrenilen Dersler

- `@Transactional` proxy tabanlı çalışır; farklı servisler arasında ortak transaction açılamaz
- Spring Data JPA method naming parsing yapar — `@Lock` ile custom metod kullanılacaksa `@Query` şart
- `@Component` filter + Security chain kaydı = double registration sorunu
- Kafka `ADVERTISED_LISTENERS=localhost` — uygulama host'ta çalışıyorsa container adı çözülemiyor
- `auto-offset-reset=earliest` sadece committed offset yoksa geçerli
- `throw e` + `@Transactional` = rollback; business failure exception değil, `ResponseEntity` ile dön
- `resilience4j-spring-boot3` Spring Boot 4.x health entegrasyonunu desteklemiyor

## Teknik Stack

| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| Spring Boot | 4.0.6 | Uygulama çatısı |
| Java | 21 | Dil |
| PostgreSQL | 16 | Kalıcı veri |
| Redis | 7 | Cache |
| Apache Kafka | 7.5.0 (Confluent) | Event streaming |
| Resilience4j | 2.3.0 | Circuit Breaker |
| MapStruct | 1.5.5 | DTO mapping |
| JJWT | 0.12.6 | JWT |
| Flyway | — | DB migration |

## Çalıştırma

```bash
# Infrastructure
docker compose up -d

# Her servis ayrı terminalde
cd account-service && mvn spring-boot:run
cd transfer-service && mvn spring-boot:run
cd fraud-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run

# Token al
curl -X POST http://localhost:9090/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```
