set danswer to text returned of (display dialog "How many hours until shutdown?" default answer "")
set tymer to 60
set hhour to tymer * tymer
delay hhour * danswer
