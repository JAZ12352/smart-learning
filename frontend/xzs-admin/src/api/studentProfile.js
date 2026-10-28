import request from '@/utils/request'

const studentProfileApi = {
  getProfile: function (userId) {
    return request({
      url: '/api/admin/student-profile/' + userId,
      method: 'get'
    })
  },
  updateProfile: function (userId, data) {
    return request({
      url: '/api/admin/student-profile/' + userId + '/update',
      method: 'post',
      data: data
    })
  },
  sendDialogue: function (userId, data, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/student-profile/' + userId + '/dialogue', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      if (!response.ok) {
        throw new Error('HTTP error! status: ' + response.status)
      }
      var reader = response.body.getReader()
      var decoder = new TextDecoder('utf-8')
      var completeContent = ''

      function read () {
        return reader.read().then(function (result) {
          if (result.done) return completeContent
          var chunk = decoder.decode(result.value, { stream: true })
          completeContent += chunk
          if (onChunk) {
            onChunk(chunk, completeContent)
          }
          return read()
        })
      }

      return read()
    })
  },
  explainProfile: function (userId, onChunk) {
    var token = localStorage.getItem('token') || ''
    return fetch('/api/admin/student-profile/' + userId + '/explain', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      credentials: 'include',
      body: JSON.stringify({})
    }).then(function (response) {
      if (!response.ok) {
        throw new Error('HTTP error! status: ' + response.status)
      }
      var reader = response.body.getReader()
      var decoder = new TextDecoder('utf-8')
      var completeContent = ''

      function read () {
        return reader.read().then(function (result) {
          if (result.done) return completeContent
          var chunk = decoder.decode(result.value, { stream: true })
          completeContent += chunk
          if (onChunk) {
            onChunk(chunk, completeContent)
          }
          return read()
        })
      }

      return read()
    })
  }
}

export default studentProfileApi
