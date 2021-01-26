# CurrencyConverterApp

### Technologies :rocket:
Written in Kotlin, based on latest recommendations by Google.
* MVVM architecture
* Realm database and extensions - save user data 
* Retrofit - for network calls
* LiveData/StateFlow - for responsive UI changes
* Coroutines - for async tasks
* Hilt DI
* Improved formatting (ktlint.gradle)

### Features v.1 (Master)
* Sync API data every 5 seconds
* Initial balance of 1000 eur can be trade to other available currency (+error handling)
* User operations and balance changes save in DB
* Exchange fee applied based on custom rules
* Alert dialog of each exchange info
* Balance updates live
* If clicked on balance, currency to sell is set up

<img width="150" src="https://user-images.githubusercontent.com/52376789/105857167-afbc2700-5ff2-11eb-8abc-8c3b9a2c32dd.png"> <img width="150" src="https://user-images.githubusercontent.com/52376789/105857161-ae8afa00-5ff2-11eb-93de-6c46da99446e.png">
