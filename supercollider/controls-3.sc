// Run this to start (DO NOT HIT COMMAND+PERIOD AFTERWARD)
(
var samplePath = PathName.new("/Users/agargara/programming/rhizome/jellyfish/jellyfish/pages/sounds/");
s = Server.local;
s.quit;
s.options.numOutputBusChannels = 32;
s.options.sampleRate = 48000;
s.options.device = "Soundflower (64ch)";
s.boot;
s.waitForBoot({
  n = NetAddr.new("pauline.mills.edu", 9000); // send messages to this address
  // n = NetAddr.new("localhost", 9000); // send messages to this address
  // n = NetAddr.new( "192.168.0.84", 9000);

  SynthDef(\agrlongtone, {
    |out=0, freq=230, time=1, gain=0.5|
    var sound, ampenv, filterenv, tremolo, depth;

    // wahwah filter
    filterenv = EnvGen.kr(Env.linen(
      Rand(time/3.1),
      Rand(time/3.1),
      Rand(time/3.1)).range(Rand(freq*0.9,freq*1.5),Rand(freq*1.5,freq*3.0))
    );

    ampenv = EnvGen.kr(Env.linen(
      Rand(time/3.1),
      Rand(time/3.1),
      Rand(time/3.1)),doneAction:2);

    sound = Select.ar(IRand(0,3), [
      Pulse.ar(freq,0.5).tanh,
      LFTri.ar(freq,0).tanh,
      Pulse.ar(freq,0.5).tanh,
      LFSaw.ar(freq,0.5).tanh
    ]);

    sound = sound*0.15; //  attenuate
    sound = RLPF.ar(sound, filterenv, LFNoise2.kr(Rand(0.001,0.1), 0.3, 0.5) );
    depth = Rand(0.1,0.2);
    tremolo = SinOsc.ar(Select.kr(IRand(0,6),[3,4,5,6,7,1,2]), 0, depth, 1-depth);
    sound = sound + (DelayC.ar(sound, 1/4,
      {Select.kr(IRand(0,3),[1/4,1/8,1/6,1/12])}.dup(2))
    * Rand(0.0,1.0)); // random delay
    5.do{sound = HPF.ar(sound, 100)}; // high pass unneeded lower freqs
    sound = sound * ampenv * tremolo * gain; // volume control
    Out.ar(out,Mix(sound.tanh)); // mono
  }).store;

  SynthDef(\agrsampler, {
    arg out=0, rate=1.0, buffer, numFrames=0, startPos = 0, endPos = 1, t_reset = 0, interpolation=1, amp=1, sampleRate=48000;
    var sig, phase;
    var length = endPos - startPos;
    phase = Line.ar(startPos * numFrames, endPos * numFrames, (numFrames/sampleRate) * BufRateScale.kr(buffer) * rate, doneAction: 2);
    sig = BufRd.ar(2, buffer, phase, 1, loop:0, interpolation:interpolation);
    Out.ar(out, (sig * amp)); // stereo out
  }).store;

  SynthDef(\agrring,{
    arg out=0,amp=0.5, atk=0.001, decay=0.01, sus=4, curve = -8, freq=100, pos=0, width=2;
    var sig, env, sil, voices=3;
    freq = freq*0.18;
    sig=PinkNoise.ar(1/(voices+1))*EnvGen.ar(Env.perc(atk,decay,1,curve));
    sig=Ringz.ar(sig*amp,Array.fill(voices,{|i|freq/((i+1)*0.09)}),sus,mul:Array.fill(voices,{|i|1/(i+2)}));
    sig=Mix(sig);
    sig=Mix.ar(GVerb.ar(sig,2,0.2,spread:0,drylevel:0,mul:0.3));
    env=EnvGen.ar(Env.linen(0.01,0,sus),doneAction:2);
    sig=Mix(sig*env);
    sig=PanAz.ar(7,sig,pos,1,width);
    Out.ar(out,sig)
  }).add;

  ~buffers = Dictionary.new;
  // load all samples in directory
  samplePath.files.do({|item, index|
    ~buffers.put(item.fileName.asSymbol, Buffer.read(s, item.fullPath));
  });
  ~renoiseBuffer = Buffer.read(s, "/Users/agargara/programming/rhizome/jellyfish/sounds/jellyfish-beat-section.wav");
  ~renoiseMelodyBuffer = Buffer.read(s, "/Users/agargara/programming/rhizome/jellyfish/sounds/jellyfish-melody-section.wav");
  ~currentMode = "bubbles";
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
  ~userList = Set.new();
  ~globalGain = 1.0;
  ~currentMode = "bubbles";
  // Function to distribute freqs in an array round-robin to each user
  ~redistributeFrequencies = {
/*    ~userList.do{
      |user, index|
      n.sendMsg("/"++user++"/freq/", ~freqs[index%~freqs.size]);
    }*/
      n.sendMsg("/everyone/freq/", ~freqs[0]);
  };
  ~playSlicedFreqsAndRedistribute={
    var slicedFreqs = ~freqs[5..10];
    n.sendMsg("/play/freqs", * slicedFreqs);
    ~redistributeFrequencies.value();
  };
  ~playFreqsAndRedistribute={
    n.sendMsg("/play/freqs", * ~freqs);
    ~redistributeFrequencies.value();
  };
  ~playManyFreqsAndRedistribute={
    n.sendMsg("/play/manyfreqs", * ~freqs);
    ~redistributeFrequencies.value();
  };
  ~setFreqsAndRedistribute={
    n.sendMsg("/set/freqs", * ~freqs);
    ~redistributeFrequencies.value();
  };
  ~playFreqs = {
    var channels = [0,1,2,3,4,5,6].scramble;
    ~freqs.do{
      |freq, i|
      Synth(\agrlongtone, [\freq, freq, \gain, 0.1, \time, 15, \out, channels[i%7]])
    }
  };
  thisProcess.openUDPPort(9002);
  n.sendMsg("/sys/subscribe",9002,"/"); // subscribe to all rhizome messages
  n.sendMsg("/mode", ~currentMode);
  // get list of all users
  //n.sendMsg("/sys/connections/sendlist", "websockets");
  thisProcess.removeOSCRecvFunc(f);

  f = { |msg, time, addr|
    if(msg[0] != '/status.reply') {
      //msg[0].postln;
      switch(msg[0],
/*        'sys/connections/websockets',{
          if(msg[1].notNil,{
            ("client list: "+msg[1]).postln;
          });
        },*/
        '/broadcast/open/websockets',{
           if(msg[1].notNil,{
            (msg[1]+" joined!").postln;
          });
        },
        '/test',
        {
          msg[1].postln;
        },
        '/addPlayer',
        {
          if(msg[1].notNil,{
            ~userList.add(msg[1].asString);
            ("Users: "+~userList).postln;
            n.sendMsg("/global/gain",~globalGain);
            ~setFreqsAndRedistribute.value();
          });
        },
        '/getPlayers',
        {
          n.sendMsg("/mode", ~currentMode);
        }
      );
    }
  };
  thisProcess.addOSCRecvFunc(f);
  Tdef(\sendCurrentMode,{
    loop{
      n.sendMsg("/mode", ~currentMode);
      1.wait;
    }
  }).play;
  ~setFreqsAndRedistribute.value();
});
)

//
//
// THE NIGHT OF LANGIS VVOLF IS UPON US
//
//
(
  // part 2 long tones
  ~currentMode = "long-tones";
  n.sendMsg("/mode", ~currentMode);
n.sendMsg("/global/gain/", 1);
Tdef(\changeFreqs, {
  var interval = 9;
  1.do{
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+4).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+8).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
  };
  1.do{
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-1).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+3).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+7).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
  };
  1.do{
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-2).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+2).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+6).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
  };
  1.do{
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-3).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+1).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
    ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+5).midicps;
    ~playSlicedFreqsAndRedistribute.value();
    ~playFreqs.value();
    interval.wait;
  };
  // PART 3: BROKEN RHYTHMS
  Tdef(\brokenDrums).play;
  Tdef(\brokenChords).embed;
  // PART 4: THE BEATZ
  Tdef(\brokenDrums).stop;
  Tdef(\extraChords).play;
  Tdef(\theBeatz).play;
  (40.96).wait;
  // PART 5: SUPERCOLLIDER BEATS + NOIZE
  Tdef(\superBeatz).play;
}).play;

Tdef(\brokenDrums,{
  var interval = 0.1;
  var step = 0;
  var brokenDrumsAmp = 0;
  ~currentMode = "broken-drums";
  n.sendMsg("/mode", ~currentMode);
  Tdef(\playDrums, {
    loop{
      var curBuf = ~buffers.values.choose;
      brokenDrumsAmp = (brokenDrumsAmp + 0.025).min(0.7);
      Synth(\agrsampler, [\buffer, curBuf, \numFrames, curBuf.numFrames,
        \sampleRate, curBuf.sampleRate, \rate, rrand(0.25,16), \amp, brokenDrumsAmp, \out, [0,1,2,3,4,5,6].choose]);
      ([1,2,3,4,5,6].choose*interval).wait;
      step = (step+1)%8;
    }
  }).play;
});

Tdef(\brokenChords,{ // @TODO change chords!!
  var interval = 9;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+4).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+8).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-1).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+3).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+7).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
});

Tdef(\extraChords,{
  var interval = 9;
  // @TODO change chords!!
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-2).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+2).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+6).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-3).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+1).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
  ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+5).midicps;
  ~playSlicedFreqsAndRedistribute.value();
  ~playFreqs.value();
  interval.wait;
});

Tdef(\theBeatz,{
  var interval = 0.08;
  var step = 0;
  var kickStep = 3;
  var snareStep = 5;
  var loopCount = 0;
  ~currentMode = "drums";
  n.sendMsg("/mode", ~currentMode);
  Tdef(\playDrums, {
    loop{
      var curBuf = ~buffers.values.choose;
      var outChannel = [0,1,2,3,4,5,6].choose;
      if((step == 0)||(step == kickStep),{
        if(1.0.rand>0.1,
          {curBuf = ~buffers.at("bd.wav".asSymbol);
            outChannel = 0;
        });
      });
      if((step == 5)||(step == snareStep),{
        if(1.0.rand>0.03,
          {curBuf = ~buffers.at("clap.wav".asSymbol);
            outChannel = 5;
        });
      });
      Synth(\agrsampler, [\buffer, curBuf, \numFrames, curBuf.numFrames,
        \sampleRate, curBuf.sampleRate, \rate, rrand(0.25,2), \amp, 0.7, \out, outChannel]);
      ([2].choose*interval).wait;
      step = (step+1)%8;
      loopCount = (loopCount+1)%128;
      if(loopCount%32 == 0,{
        snareStep = [2,5,7].wchoose([4,10,1].normalizeSum);
        kickStep = [2,3,4,5,7].wchoose([3,5,1,1,2].normalizeSum);
      });
    }
  }).play;
});

Tdef(\superBeatz,{
var interval = 0.08;
var step = 0;
var kickStep = 3;
var snareStep = 5;
var loopCount = 0;
  Tdef(\playDrums, {
    var renoiseAmp = 0;
    // start renoise beats
    ~renoiseSynth = Synth(\agrsampler, [\buffer, ~renoiseBuffer, \numFrames, ~renoiseBuffer.numFrames,
      \sampleRate, ~renoiseBuffer.sampleRate, \rate, 1, \amp, 0, \out, 0]);
    636.do{ // 637.5 loops = 102 seconds
      |i|
      var curBuf = ~buffers.values.choose;
      var outChannel = [0,1,2,3,4,5,6].choose;
      // Fade in renoise beats
      renoiseAmp = (renoiseAmp+0.01).min(1.0);
      ~renoiseSynth.set(\amp, renoiseAmp);
      if((step == 0)||(step == kickStep),{
        if(1.0.rand>0.1,
          {curBuf = ~buffers.at("bd.wav".asSymbol);
            outChannel = 0;
        });
      });
      if((step == 5)||(step == snareStep),{
        if(1.0.rand>0.03,
          {curBuf = ~buffers.at("clap.wav".asSymbol);
            outChannel = 5;
        });
      });
      Synth(\agrsampler, [\buffer, curBuf, \numFrames, curBuf.numFrames,
        \sampleRate, curBuf.sampleRate, \rate, rrand(0.25,2), \amp, 0.7, \out, outChannel]);
      ([2].choose*interval).wait;
      step = (step+1)%8;
      loopCount = (loopCount+1)%128;
      if(loopCount%32 == 0,{
        snareStep = [2,5,7].wchoose([4,10,1].normalizeSum);
        kickStep = [2,3,4,5,7].wchoose([3,5,1,1,2].normalizeSum);
      });
      if(i>318,{
        // halfway through, change to noise
        ~currentMode = "noise";
        n.sendMsg("/mode", ~currentMode);
      });
    };
    Tdef(\melodicSection).play;
    // start renoise melodic section
    ~renoiseSynth = Synth(\agrsampler, [\buffer, ~renoiseMelodyBuffer, \numFrames, ~renoiseMelodyBuffer.numFrames,
      \sampleRate, ~renoiseMelodyBuffer.sampleRate, \rate, 1, \amp, 1, \out, 8]);
  }).play;
});

Tdef(\melodicSection,{
  var interval = 5.12;
  ~currentMode = "rising-fm";
  n.sendMsg("/mode", ~currentMode);
  1.do{
    ~freqs = ([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]++([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]++([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]++([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 51, 54, 56, 58, 61, 63, 65, 66, 68, 70, 73, 77, 78 ]++([ 51, 54, 56, 58, 61, 63, 65, 66, 68, 70, 73, 77, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]++([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]++([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]++([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 51, 54, 56, 58, 61, 63, 65, 66, 68, 70, 73, 77, 78 ]++([ 51, 54, 56, 58, 61, 63, 65, 66, 68, 70, 73, 77, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 49, 51, 53, 56, 58, 60, 61, 63, 65, 68, 70 ]++([ 48, 49, 51, 53, 56, 58, 60, 61, 63, 65, 68, 70 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 51, 53, 54, 56, 58, 59, 61, 63, 65, 66, 68, 70, 71 ]++([ 49, 51, 53, 54, 56, 58, 59, 61, 63, 65, 66, 68, 70, 71 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 50, 53, 57, 59, 60, 62, 65, 69, 71, 72 ]++([ 48, 50, 53, 57, 59, 60, 62, 65, 69, 71, 72 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 51, 53, 55, 58, 60, 63, 65, 67, 70, 72 ]++([ 48, 51, 53, 55, 58, 60, 63, 65, 67, 70, 72 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 50, 54, 55, 57, 59, 61, 62, 66, 67, 69, 71, 73, 74 ]++([ 50, 54, 55, 57, 59, 61, 62, 66, 67, 69, 71, 73, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 50, 54, 57, 59, 62, 66, 69, 71, 74 ]++([ 50, 54, 57, 59, 62, 66, 69, 71, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 52, 53, 57, 59, 60, 62, 64, 65, 69, 71, 74 ]++([ 48, 52, 53, 57, 59, 60, 62, 64, 65, 69, 71, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 50, 52, 54, 57, 59, 62, 64, 66, 69, 71, 74 ]++([ 50, 52, 54, 57, 59, 62, 64, 66, 69, 71, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 51, 53, 56, 58, 60, 63, 65, 68, 70, 72 ]++([ 48, 51, 53, 56, 58, 60, 63, 65, 68, 70, 72 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 50, 52, 54, 57, 60, 62, 64, 66, 69, 72 ]++([ 48, 50, 52, 54, 57, 60, 62, 64, 66, 69, 72 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 51, 54, 56, 58, 59, 61, 63, 66, 68, 70, 71 ]++([ 49, 51, 54, 56, 58, 59, 61, 63, 66, 68, 70, 71 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 50, 53, 57, 58, 60, 62, 65, 69, 70, 72 ]++([ 48, 50, 53, 57, 58, 60, 62, 65, 69, 70, 72 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]++([ 49, 50, 54, 56, 57, 61, 62, 66, 68, 69, 73, 74 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]++([ 49, 50, 54, 57, 59, 61, 62, 66, 69, 71, 74, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]++([ 49, 53, 54, 58, 59, 61, 63, 65, 66, 70, 71, 75, 78 ]+12)).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~currentMode = "loopy";
    n.sendMsg("/mode", ~currentMode);
    ~freqs = ([ 51, 54, 56, 58, 61, 63, 65, 66, 68, 70, 73, 77, 78 ]).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 49, 51, 53, 56, 58, 60, 61, 63, 65, 68, 70 ]).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 49, 51, 53, 54, 56, 58, 59, 61, 63, 65, 66, 68, 70, 71 ]).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;
    ~freqs = ([ 48, 50, 53, 57, 59, 60, 62, 65, 69, 71, 72 ]).asSet.asArray.sort.midicps;
    ~playManyFreqsAndRedistribute.value();
    interval.wait;

    // PART 7 - LOOPY
    Tdef(\loopySection).play;
    Tdef(\agrRingMe).play;
  };
});

Tdef(\loopySection,{
  var interval = 2.56;
  ~freqs = ([57, 69, 62, 65, 71, 60, 48, 72]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([57, 69, 62, 64, 72, 60, 46]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([57, 69, 62, 64, 71, 45]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([57, 69, 60, 64, 71]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([60, 69, 64, 66, 71]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([59, 69, 64, 66, 73, 71]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([57, 68, 64, 61, 73]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/2).wait;
  ~freqs = ([58, 69, 65]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([56, 58, 62, 64]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/2).wait;
  ~freqs = ([62, 63]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([60, 63, 65]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([60, 63, 65, 67]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([62, 63, 69, 67, 62, 72]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([64, 74, 71, 62, 76]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([69, 67, 62, 72, 75, 81]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([70, 67, 62, 75, 81]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([70, 67, 60, 75, 62]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([58, 60, 74]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([56, 77]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([79, 75, 70, 74, 63]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([79, 60, 70, 62]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/4).wait;
  ~freqs = ([79, 60, 72, 63]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/4).wait;
  ~freqs = ([63, 60, 72, 62]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/4).wait;
  ~freqs = ([63, 60, 72, 70, 61]).midicps;
  ~setFreqsAndRedistribute.value();
  (interval/4).wait;
  ~freqs = ([62, 60, 74, 71]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([62, 59, 74]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([62, 57, 73, 71, 66]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([61, 56, 73, 71, 66, 80]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([54,64,57,80,61]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  Tdef(\craziness1).embed;
  ~freqs = ([53,63,56,82,61]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([55,58,60]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([39,58,60]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([41,63,58,68,60,65]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([40,61,56,68,61]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([39,60,53]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([38,60,53]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([37,60,53,58,72,65]).midicps; // @TODO add more of these nice low note patterns :)
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([36,58,72]).midicps;
  ~setFreqsAndRedistribute.value();
  interval.wait;
  ~freqs = ([36,58]).midicps;
  ~setFreqsAndRedistribute.value();
    interval.wait;
  ~freqs = ([36]).midicps;
  ~setFreqsAndRedistribute.value();
  (10).wait;
  ~currentMode = "one-note";
  n.sendMsg("/mode", ~currentMode);
  20.wait;
  Tdef(\agrRingMe).stop;
  40.wait;
  Tdef(\fadeOutGlobalGain).play;
  15.wait;
  ~currentMode = "the-end";
  n.sendMsg("/mode", ~currentMode);
});

Tdef(\fadeOutGlobalGain,{
  100.do{
    ~globalGain = (~globalGain - 0.01).max(0);
    n.sendMsg("/global/gain", ~globalGain);
    0.1.wait;
  };
});

Tdef(\fadeInGlobalGain,{
  100.do{
    ~globalGain = (~globalGain + 0.01).min(1.0);
    n.sendMsg("/global/gain", ~globalGain);
    0.1.wait;
  };
});

Tdef(\agrRingMe,{
  var channels = [0,1,2,3,4,5,6];
  var interval = 0.16;
  var currentFreq = 0;
  var i = 0;
  loop{
    Synth(\agrring, [\pos, rrand(-2.0,2.0), \amp, 0.1, \sus, 2, \freq, ~freqs[currentFreq%~freqs.size]]);
    interval.wait;
    i = (i+1)%7;
    currentFreq = (currentFreq+1)%~freqs.size;
  };
});

Tdef(\craziness1,{
  var interval = 2.56;
  8.do{
    ~freqs = ([52]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([54]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([57]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([59]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([66]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([62]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
  };
  8.do{
    ~freqs = ([53]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([56]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([82]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([61]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([63]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
  };
  8.do{
    ~freqs = ([51]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([65]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([54]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([56]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([59]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([61]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
  };
  8.do{
    ~freqs = ([50]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([69]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([80]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([59]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([54]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([61]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
    ~freqs = ([57]).midicps;
    ~setFreqsAndRedistribute.value();
    (interval/16).wait;
  };
});
)



//
// random tests
//

(
Tdef(\melodicSection).play;
~renoiseSynth = Synth(\agrsampler, [\buffer, ~renoiseMelodyBuffer, \numFrames, ~renoiseMelodyBuffer.numFrames,
      \sampleRate, ~renoiseMelodyBuffer.sampleRate, \rate, 1, \amp, 1, \out, 8]);
)
Tdef(\melodicSection).stop;
Tdef(\agrRingMe).play;
Tdef(\agrRingMe).stop;
Tdef(\loopySection).play;
Tdef(\loopySection).stop;
Tdef(\craziness1).play;
Tdef(\craziness1).stop;
Tdef(\changeFreqs).stop;

~freqs = ([50,69,80,59,54,61,57]).midicps;
~setFreqsAndRedistribute.value();


Synth(\agrring, [\pos, rrand(-2.0,2.0), \amp, 0.1, \sus, 2, \freq, 400*0.18]);
Synth(\agrping, [ \freq, 400]);

~currentMode = "bubbles";
n.sendMsg("/mode", ~currentMode);
~currentMode = "long-tones";
n.sendMsg("/mode", ~currentMode);
~currentMode = "broken-drums";
n.sendMsg("/mode", ~currentMode);
~currentMode = "drums";
n.sendMsg("/mode", ~currentMode);
~currentMode = "rising-fm";
n.sendMsg("/mode", ~currentMode);
~currentMode = "loopy";
n.sendMsg("/mode", ~currentMode);
~currentMode = "one-note";
n.sendMsg("/mode", ~currentMode);
~currentMode = "the-end";
n.sendMsg("/mode", ~currentMode);
~globalGain = 1;
n.sendMsg("/global/gain", ~globalGain);