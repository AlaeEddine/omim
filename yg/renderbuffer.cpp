#include "../base/SRC_FIRST.hpp"

#include "internal/opengl.hpp"
#include "renderbuffer.hpp"
#include "utils.hpp"

#include "../base/logging.hpp"

#include "../std/list.hpp"

namespace yg
{
  namespace gl
  {
    list<unsigned int> renderBufferStack;

    int RenderBuffer::current()
    {
      int id;
#ifdef OMIM_GL_ES
      OGLCHECK(glGetIntegerv(GL_RENDERBUFFER_BINDING_OES, &id));
#else
      OGLCHECK(glGetIntegerv(GL_RENDERBUFFER_BINDING_EXT, &id));
#endif
      return id;
    }

    void RenderBuffer::checkID() const
    {
      if (!m_hasID)
      {
        m_hasID = true;
#ifdef OMIM_GL_ES
        OGLCHECK(glGenRenderbuffersOES(1, &m_id));
        makeCurrent();

        GLenum target = GL_RENDERBUFFER_OES;
        GLenum internalFormat = m_isDepthBuffer ? GL_DEPTH_COMPONENT24_OES : GL_RGBA8_OES;

        OGLCHECK(glRenderbufferStorageOES(target,
                                          internalFormat,
                                          m_width,
                                          m_height));
#else
        OGLCHECK(glGenRenderbuffers(1, &m_id));
        makeCurrent();

        GLenum target = GL_RENDERBUFFER_EXT;
        GLenum internalFormat = m_isDepthBuffer ? GL_DEPTH_COMPONENT24 : GL_RGBA8;

        OGLCHECK(glRenderbufferStorageEXT(target,
                                          internalFormat,
                                          m_width,
                                          m_height));

#endif
      }
    }

    RenderBuffer::RenderBuffer(size_t width, size_t height, bool isDepthBuffer)
      : m_isDepthBuffer(isDepthBuffer), m_width(width), m_height(height), m_hasID(false), m_id(0)
    {}

    RenderBuffer::~RenderBuffer()
    {
      if (m_hasID)
      {
#ifdef OMIM_GL_ES
        OGLCHECK(glDeleteRenderbuffersOES(1, &m_id));
#else
        OGLCHECK(glDeleteRenderbuffersEXT(1, &m_id));
#endif
      }
    }

    unsigned int RenderBuffer::id() const
    {
      checkID();
      return m_id;
    }

    void RenderBuffer::attachToFrameBuffer()
    {
      checkID();
#ifdef OMIM_GL_ES
      OGLCHECK(glFramebufferRenderbufferOES(
          GL_FRAMEBUFFER_OES,
          isDepthBuffer() ? GL_DEPTH_ATTACHMENT_OES : GL_COLOR_ATTACHMENT0_OES,
          GL_RENDERBUFFER_OES,
          id()));
#else
      OGLCHECK(glFramebufferRenderbuffer(
          GL_FRAMEBUFFER_EXT,
          isDepthBuffer() ? GL_DEPTH_ATTACHMENT_EXT : GL_COLOR_ATTACHMENT0_EXT,
          GL_RENDERBUFFER_EXT,
          id()));
#endif
      if (!isDepthBuffer())
        utils::setupCoordinates(width(), height(), false);
    }

    void RenderBuffer::makeCurrent() const
    {
      checkID();
#ifndef OMIM_OS_ANDROID
      if (m_id != current())
#endif
      {
#ifdef OMIM_GL_ES
        OGLCHECK(glBindRenderbufferOES(GL_RENDERBUFFER_OES, m_id));
#else
        OGLCHECK(glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, m_id));
#endif
      }
    }

    bool RenderBuffer::isDepthBuffer() const
    {
      return m_isDepthBuffer;
    }

    unsigned RenderBuffer::width() const
    {
      return m_width;
    }

    unsigned RenderBuffer::height() const
    {
      return m_height;
    }
  }
}
