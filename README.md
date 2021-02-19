# Tubes1-Stima-Worms
## Penjelasan singkat algoritma greedy yang diimplementasikan
Algoritma greedy adalah suatu algoritma yang memecahkan suatu persoalan langkah demi langkah dengan memilih pilihan terbaik di setiap langkahnya dengan harapan pilihan terbaik tersebut akan memberikan solusi akhir yang terbaik. Pada bot ini, strategi algoritma greedy diimplementasikan dengan cara memilih command yang akan memaksimumkan damage yang diterima lawan dan meminimumkan damage yang diterima pemain.
(MUNGKIN BISA TAMBAHIN LAGI)
## Requirement program
Untuk menjalankan program, dibutuhkan beberapa requirement yaitu:
- Java (minimal Java 8), dapat diunduh melalui https://www.oracle.com/java/technologies/javase/javasejdk8-downloads.html
- IntelliJ IDEA, dapat diunduh melalui https://www.jetbrains.com/idea/
- NodeJS, dapat diunduh melalui https://nodejs.org/en/download/
## Cara menggunakan program
(Masih belom fix tergantung struktur folder pengumpulan)
1. Unzip folder Tubes1_13519040.zip
2. Copy folder Tubes1_13519040 yang berada di dalam folder src ke dalam folder starter-bots dari starter-pack yang telah Anda download
3. Untuk menjalankan permainan dengan menggunakan bot kami, ubah isi file game-runner-config.json pada folder starter-pack menjadi seperti berikut:
```
{
  "round-state-output-location": "./match-logs",
  "game-config-file-location": "game-config.json",
  "game-engine-jar": "./ec-2019-game-engine-jvm-full-2019.3.2.jar",
  "verbose-mode": true,
  "max-runtime-ms": 1000,
  "player-a": "./reference-bot/javascript",
  "player-b": "./starter-bots/Tubes1_13519040",
  "max-request-retries": 10,
  "request-timeout-ms": 5000,
  "is-tournament-mode": false,
  "tournament": {
    "connection-string": "",
    "bots-container": "",
    "match-logs-container": "",
    "game-engine-container": "",
    "api-endpoint": "http://localhost"
  }
}

```
5. Buka terminal dan pergi ke directory starter-pack
6. Ketik `make run` pada terminal jika Anda menggunakan Mac OS/Linux atau klik dua kali run.bat jika Anda menggunakan Windows untuk menjalankan permainan

## Cara menggunakan visualizer
Visualizer hanya dapat digunakan di Windows. Langkah-langkahnya adalah sebagai berikut:
1. Unduh visualizer melalui https://github.com/dlweatherhead/entelect-challenge-2019-visualiser/releases/tag/v1.0f1
2. Pindahkan folder hasil running permainan yang ingin divisualisasikan dari starter-pack/match-logs ke folder EC2019 Final v1.0f1/Matches
3. Jalankan file entelect-visualizer.exe yang berada di dalam folder EC2019 Final v1.0f1 dan pilih match yang ingin Anda lihat visualisasinya
## Identitas Pembuat
Kelompok Bot.java
- 13519040 Shafira Naya Aprisadianti
- 13519054 Aisyah Farras Aqila
- 13519192 Gayuh Tri Rahutami
