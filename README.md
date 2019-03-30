# mailfetcher

A script written in clojure to load data for a bee hive from a google account using a client secret for authentication.

The header of the messages has to be:
"Report YYYY-MM-DD hh:mm:ss"

The format of the messages needs to be similar to:

"
Scale: Jastrebac 2
NET: 47.17kg
TARA: 27.32kg
DI: -0.02kg
GSM Strength: 11 (7 to 31)
Sys.Temp: 8C
Battery level: 4/5+++
"

Used with this kind of bee hive scale https://smsvaga.com/index.php/en/


Notes fot future:

Don't use googles api. The messages are probably much easier to get automatically with some cli tool than trying to use this thing. 

Why does this exist?
It was an excercise for me on clojure. 
The scale sends the information to an email address. I wanted to make something useful out of the data. There might be some way to hack the device to do what you actually want but this was the way I ended doing it. It also could send the messages trough plain ftp, but that would require me to have a plain open ftp server somewhere which seems like a bad idea.

Who might this be useful for?
You might be able to use this as an example on how to get google api to work with clojure. I don't think my solution is very amazing but it works more or less.

## Installation

in the beginning of the core.js there are a couple of fields where required atuhentication related information file paths have to be added.

lein deps
lein install
lein run
