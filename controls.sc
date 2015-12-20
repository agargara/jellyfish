(
var userList = Set.new();
n = NetAddr.new("localhost", 9000); // send messages to this address
thisProcess.openUDPPort(9002);
n.sendMsg("/sys/subscribe", 9002, "/"); // subscribe to all rhizome messages
thisProcess.removeOSCRecvFunc(f);
f = { |msg, time, addr|
  if(msg[0] != '/status.reply') {
    switch(msg[0],
      '/addPlayer',{
        if(msg[1].notNil,{
          userList.add(msg[1]);
          ("Users: "+userList).postln;
        });
      }
    );
  }
};
OSCdef.new(\freqListener,
  {|msg, time, addr, recvPort|
    if(msg[3].notNil,{n.sendMsg("/freq",msg[3].asFloat);});
}, '/freq');
OSCdef.new(\sliderListener,
  {|msg, time, addr, recvPort|
    if(msg[3].notNil,{n.sendMsg("/slider1",msg[3].asFloat);});
}, '/slider1');
thisProcess.addOSCRecvFunc(f);
)

(
r = {
  var trig = Impulse.kr(2);
  var addy = TRand.kr(200,400,trig);
  var freq = TRand.kr(100,1000,trig).round(150);
  var amp = Linen.kr(trig,0,1,2);
  SendReply.kr(
    trig,
    '/freq',
    freq
  );
  SendReply.kr(
    Impulse.kr(64),
    '/slider1',
    amp
  );
}.play(s);
)
r.free;


(
r = {
  var freqTrig, ampTrig, freq, lagFreq, amp, lagAmp, trgDur=0.01;
  var addTrig = Impulse.kr(2);
  var add = TRand.kr(200,400,addTrig);
  // Freq trigger
  freq = SinOsc.kr(3,0,40,add);
  lagFreq = Lag.kr(freq,trgDur);
  freqTrig = Trig1.kr(((freq-1)>lagFreq)+(lagFreq>(freq+1)),trgDur);
  // Amp trigger
  amp = Linen.kr(addTrig,0,1,10);
  lagAmp = Lag.kr(amp,trgDur);
  ampTrig = Trig1.kr(((amp-1)>lagAmp)+(lagAmp>(amp+1)),trgDur);
  SendReply.kr(
    freqTrig,
    '/freq',
    freq
  );
  SendReply.kr(
    ampTrig,
    '/slider1',
    amp
  );
}.play(s);
)
r.free;


// For testing
n.sendMsg("/slider1", 0.85); //ON
n.sendMsg("/slider1", 0.0);  //OFF
n.sendMsg("/freq", 200);

(
Tdef(\changeFreq, {
  loop{
    forBy(100, 1000, 10, {
      arg i;
      n.sendMsg("/freq", i);
      0.5.wait;
    });
  }
}).play;
)