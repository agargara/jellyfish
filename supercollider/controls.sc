// Run this to start (DO NOT HIT COMMAND+PERIOD AFTERWARD)
(
~userList = Set.new();
n = NetAddr.new("pauline.mills.edu", 9000); // send messages to this address
thisProcess.openUDPPort(9002);
n.sendMsg("/sys/subscribe", 9002, "/"); // subscribe to all rhizome messages
thisProcess.removeOSCRecvFunc(f);
f = { |msg, time, addr|
  if(msg[0] != '/status.reply') {
    switch(msg[0],
      '/addPlayer',{
        if(msg[1].notNil,{
          ~userList.add(msg[1].asString);
          ("Users: "+~userList).postln;
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

// Distribute freqs in an array round-robin to each user
(
~freqs = [130.8127826503, 146.8323839587, 155.56349186104, 174.6141157165, 195.99771799087, 207.65234878997, 233.08188075904, 261.6255653006, 293.66476791741, 311.12698372208, 349.228231433, 391.99543598175, 415.30469757995, 466.16376151809, 587.32953583482];
~userList.do{
  |user, index|
  n.sendMsg("/"++user++"/freq/", ~freqs[index%~freqs.size]);
}
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
n.sendMsg("/mode", "rising-fm");  //OFF
n.sendMsg("/mode", "noise");  //OFF

([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+8).midicps;

(
Tdef(\changeFreqs, {
  loop{
    n.sendMsg("/freqs", 130.8127826503, 146.8323839587, 155.56349186104, 174.6141157165, 195.99771799087, 207.65234878997, 233.08188075904, 261.6255653006, 293.66476791741, 311.12698372208, 349.228231433, 391.99543598175, 415.30469757995, 466.16376151809, 587.32953583482);
    6.wait;
    n.sendMsg("/freqs",  164.81377845643, 184.99721135582, 195.99771799087, 220, 246.94165062806, 261.6255653006, 293.66476791741, 329.62755691287, 369.99442271163, 391.99543598175, 440, 493.88330125612, 523.2511306012, 587.32953583482, 739.98884542327);
    6.wait;
    n.sendMsg("/freqs",  207.65234878997, 233.08188075904, 246.94165062806, 277.18263097687, 311.12698372208, 329.62755691287, 369.99442271163, 415.30469757995, 466.16376151809, 493.88330125612, 554.36526195374, 622.25396744416, 659.25511382574, 739.98884542327, 932.32752303618 );
    6.wait;
  }
}).play;
)

Tdef(\changeFreqs).stop;

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