function VCO(context, type, freq, gain){
  this.oscillator = context.createOscillator();
  this.oscillator.type = type;
  this.input = this.oscillator;
  this.gain = context.createGain();
  this.gain.gain.value = gain;
  this.output = this.gain;
  this.setFrequency = function(freq, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.oscillator.frequency.setValueAtTime(freq, time);
  };
  this.rampToFrequency = function(freq, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.oscillator.frequency.linearRampToValueAtTime(freq, time);
  };
  this.setGain = function(gain, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.gain.gain.setValueAtTime(gain, time);
  };
  this.rampToGain = function(gain, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.gain.gain.linearRampToValueAtTime(gain, time);
  };
  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
  this.setFrequency(freq);
  this.oscillator.connect(this.gain);
};

function Pulse_VCO(context, freq, gain){
  this.oscillator = context.createPulseOscillator();
  this.input = this.oscillator;
  this.gain = context.createGain();
  this.gain.gain.value = gain;
  this.output = this.gain;
  this.setFrequency = function(freq, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.oscillator.frequency.setValueAtTime(freq, time);
  };
  this.rampToFrequency = function(freq, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.oscillator.frequency.linearRampToValueAtTime(freq, time);
  };
  this.setGain = function(gain, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.gain.gain.setValueAtTime(gain, time);
  };
  this.rampToGain = function(gain, time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.gain.gain.linearRampToValueAtTime(gain, time);
  };
  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
  this.setFrequency(freq);
  this.oscillator.connect(this.gain);
};

function VCA(context) {
  this.gain = context.createGain();
  this.gain.gain.value = 0;
  this.input = this.gain;
  this.output = this.gain;
  this.amplitude = this.gain.gain;
  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  }
};

function PinkNoise(context, gain){
  var bufferSize = 4096;
  var b0, b1, b2, b3, b4, b5, b6;
  this.gain = gain;
  b0 = b1 = b2 = b3 = b4 = b5 = b6 = 0.0;
  this.node = context.createScriptProcessor(bufferSize, 1, 1);
  this.output = this.node;
  this.node.onaudioprocess = function(e) {
    var output = e.outputBuffer.getChannelData(0);
    for (var i = 0; i < bufferSize; i++) {
      var white = Math.random() * 2 - 1;
      b0 = 0.99886 * b0 + white * 0.0555179;
      b1 = 0.99332 * b1 + white * 0.0750759;
      b2 = 0.96900 * b2 + white * 0.1538520;
      b3 = 0.86650 * b3 + white * 0.3104856;
      b4 = 0.55000 * b4 + white * 0.5329522;
      b5 = -0.7616 * b5 - white * 0.0168980;
      output[i] = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362;
      output[i] *= 0.11 * gain; // (roughly) compensate for gain
      b6 = white * 0.115926;
    }
  }
  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
  this.disconnect = function(node) {
    this.output.disconnect();
  };
}

function WhiteNoise(context, gain){
  this.gainNode = context.createGain();
  this.gainNode.gain.value = gain;
  this.node = context.createBufferSource();
  this.buffer = context.createBuffer(1, 88200, 22050+Math.floor((Math.random()*22050))); //context.sampleRate);
  this.data = this.buffer.getChannelData(0);
  for (var i = 0; i < 88200; i++) {
    if(i%5 == 1){
      this.data[i] = 1;
    }else if(i%5 == 0){
      this.data[i] = -1;
    }else{
      this.data[i] = Math.random();
    }
  }
  this.node.buffer = this.buffer;
  this.node.loop = true;
  this.node.connect(this.gainNode);
  this.output = this.gainNode;

  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
  this.disconnect = function(node) {
    this.output.disconnect();
  };
}

function AR_EnvelopeGenerator(context, peak) {
  this.attackTime = 0.1;
  this.releaseTime = 0.1;
  this.peak = peak;

  this.trigger = function(time) {
    time = typeof time !== 'undefined' ? time : context.currentTime;
    this.gain.cancelScheduledValues(time);
    this.gain.setValueAtTime(0, time);
    this.gain.linearRampToValueAtTime(this.peak, time + this.attackTime);
    this.gain.linearRampToValueAtTime(0, time + this.attackTime + this.releaseTime);
  };

  this.connect = function(gain) {
    this.gain = gain;
  };
};

function ASR_EnvelopeGenerator(context, peak) {
  this.attackTime = 0.1;
  this.sustainLevel = 0.5;
  this.releaseTime = 0.3;
  this.peak = peak;
  this.gainNode = context.createGain();
  this.gainNode.gain.value = 0.01;
  this.input = this.gainNode;
  this.output = this.gainNode;

  this.trigger = function() {
    var now = context.currentTime;
    this.gainNode.gain.cancelScheduledValues(now);
    this.gainNode.gain.setValueAtTime(0.01, now);
    this.gainNode.gain.exponentialRampToValueAtTime(this.peak, now + this.attackTime);
  };

  this.release = function(){
    var now = context.currentTime;
    var currentGain = this.gainNode.gain.value;
    this.gainNode.gain.cancelScheduledValues(now);
    this.gainNode.gain.setValueAtTime(currentGain, now);
    this.gainNode.gain.linearRampToValueAtTime(0.00001, now + this.releaseTime);
  }

  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
};

function Distortion(context){
  this.distortion = context.createWaveShaper();
  function makeDistortionCurve(amount) {
    var k = typeof amount === 'number' ? amount : 50,
      n_samples = 44100,
      curve = new Float32Array(n_samples),
      deg = Math.PI / 180,
      i = 0,
      x;
    for ( ; i < n_samples; ++i ) {
      x = i * 2 / n_samples - 1;
      curve[i] = ( 3 + k ) * x * 20 * deg / ( Math.PI + k * Math.abs(x) );
    }
    return curve;
  };
  this.distortion.curve = makeDistortionCurve(400);
  this.distortion.oversample = '4x';
  this.output = this.distortion;
  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };
}