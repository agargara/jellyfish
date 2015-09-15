(
(
b = NetAddr.new("localhost", 9000); // send messages to this address
thisProcess.removeOSCRecvFunc(f);
f = { |msg, time, addr|
  if(msg[0] != '/status.reply') {
    if(msg[0] == '/freq', {
      if(msg[3].isNil.not, {
        b.sendMsg("/freq",
          msg[3].asFloat);
      });
    },{
      if(msg[0] == '/slider1', {
      if(msg[3].isNil.not, {
        b.sendMsg("/slider1",
          msg[3].asFloat);
      });
      });
    });
  }
};
thisProcess.addOSCRecvFunc(f);
);
)

(
OSCdef.newMatching(\freqListener, {|msg, time, addr, recvPort| msg.postln}, '/freq');
)

(
r = {
  var trig = Impulse.kr(0.5);
  var addy = TRand.kr(200,400,trig);
  SendReply.ar(
    Impulse.ar(1000),
    '/freq',
    SinOsc.ar(3,0,40,addy)
  );
  SendReply.kr(
    Impulse.kr(20),
    '/slider1',
    Linen.kr(trig,0,1,10)
  );
}.play(s);
)
r.free;

// Now sending stuff
b.sendMsg("/slider1", 0.85);
b.sendMsg("/slider1", 0.15);
b.sendMsg("/freq", 50);

b.sendMsg("/freq", SinOsc.ar(1,0,50,100));
