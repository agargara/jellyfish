fields.config = function () {

    return {
      'simple-osc': {
        instrument: 'WebPdInstrument',
        args: ['assets/patches/simple-osc.pd']
      },
      'simple-file-playback': {
        instrument: 'WebPdInstrument',
        args: ['assets/patches/simple-file-playback.pd']
      }
    }

  }