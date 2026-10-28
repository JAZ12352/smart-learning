import request from '@/utils/request'

function readStream (response, onChunk) {
  if (!response.ok) {
    throw new Error('HTTP error! status: ' + response.status)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let completeContent = ''

  function read () {
    return reader.read().then(function (result) {
      if (result.done) return completeContent
      const chunk = decoder.decode(result.value, { stream: true })
      completeContent += chunk
      if (onChunk) {
        onChunk(chunk, completeContent)
      }
      return read()
    })
  }

  return read()
}

const learningPathApi = {
  getPath: function (userId, subjectId) {
    return request({
      url: '/api/admin/learning-path/' + userId + '/' + subjectId,
      method: 'get'
    })
  },
  generate: function (data, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/learning-path/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      return readStream(response, onChunk)
    })
  },
  updateProgress: function (data) {
    return request({
      url: '/api/admin/learning-path/progress',
      method: 'post',
      data: data
    })
  }
}

export default learningPathApi
