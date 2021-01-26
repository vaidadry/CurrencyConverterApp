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

<img width="150" src="https://user-images.githubusercontent.com/52376789/105849554-35d37000-5fe9-11eb-9c09-8023542cfac2.png"> <img width="150" src="https://user-images.githubusercontent.com/52376789/105849632-4daaf400-5fe9-11eb-874b-fd5931a55be8.png">
